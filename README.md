# Saga Orquestrado com Microsserviços

Implementação do padrão SAGA Orquestrado com Java, Spring Boot e Apache Kafka. O projeto cobre o fluxo completo de um pedido  validação de produtos, pagamento e atualização de estoque  com compensação automática (rollback) em caso de falha em qualquer etapa.

## Arquitetura

```
                       ┌─────────────────────┐
                       │    Order Service     │ ← POST /api/order
                       │    porta 3000        │
                       │    MongoDB           │
                       └──────────┬──────────┘
                                  │  start-saga
                                  ▼
                       ┌─────────────────────┐
                       │ Orchestrator Service │
                       │    porta 8080        │
                       │ SagaExecutionController
                       └──────────┬──────────┘
            ┌────────────┬────────┴──────────────┐
            ▼            ▼                        ▼
  ┌──────────────┐  ┌────────────┐  ┌──────────────────┐
  │   Product    │  │  Payment   │  │   Inventory      │
  │  Validation  │  │  Service   │  │   Service        │
  │  porta 8090  │  │ porta 8091 │  │   porta 8092     │
  │  PostgreSQL  │  │ PostgreSQL │  │   PostgreSQL     │
  └──────────────┘  └────────────┘  └──────────────────┘
```

### Serviços

| Serviço | Porta | Banco | Responsabilidade |
|---|---|---|---|
| order-service | 3000 | MongoDB | Entrada da SAGA: recebe o pedido e publica o evento inicial |
| orchestrator-service | 8080 | sem banco | Coordena o fluxo e decide o próximo passo a cada resposta |
| product-validation-service | 8090 | PostgreSQL | Valida se os produtos do pedido existem |
| payment-service | 8091 | PostgreSQL | Processa o pagamento |
| inventory-service | 8092 | PostgreSQL | Dá baixa no estoque |

### Tópicos Kafka

| Tópico | Produzido por | Consumido por |
|---|---|---|
| `start-saga` | order-service | orchestrator-service |
| `orchestrator` | todos os serviços | orchestrator-service |
| `product-validation-success` | orchestrator-service | product-validation-service |
| `product-validation-fail` | orchestrator-service | product-validation-service |
| `payment-success` | orchestrator-service | payment-service |
| `payment-fail` | orchestrator-service | payment-service |
| `inventory-success` | orchestrator-service | inventory-service |
| `inventory-fail` | orchestrator-service | inventory-service |
| `finish-success` | orchestrator-service | (fim da saga) |
| `finish-fail` | orchestrator-service | (fim da saga) |
| `notify-ending` | orchestrator-service | order-service |

## Fluxo da SAGA

### Fluxo de sucesso

```
Order Service       →  [start-saga]                  →  Orchestrator
Orchestrator        →  [product-validation-success]   →  Product Validation Service
Product Validation  →  [orchestrator: SUCCESS]        →  Orchestrator
Orchestrator        →  [payment-success]              →  Payment Service
Payment Service     →  [orchestrator: SUCCESS]        →  Orchestrator
Orchestrator        →  [inventory-success]            →  Inventory Service
Inventory Service   →  [orchestrator: SUCCESS]        →  Orchestrator
Orchestrator        →  [finish-success] + [notify-ending]
```

### Fluxo de compensação

Se qualquer serviço retornar falha, o orquestrador aciona o rollback em cadeia reversa. Exemplo com falha no inventory:

```
Inventory Service  →  ROLLBACK_PENDING  →  inventory-fail   (rollback do próprio serviço)
Inventory Service  →  FAIL              →  payment-fail     (rollback do pagamento)
Payment Service    →  FAIL              →  product-validation-fail
                                        →  finish-fail
```

### SagaHandler  matriz de decisão

O orquestrador não usa `if/else` para rotear os eventos. Toda a lógica está concentrada em uma matriz estática: dado o serviço de origem e o status do evento, existe exatamente um tópico de destino.

```java
{ ORCHESTRATOR,               SUCCESS,          PRODUCT_VALIDATION_SUCCESS },
{ ORCHESTRATOR,               FAIL,             FINISH_FAIL                },
{ PRODUCT_VALIDATION_SERVICE, ROLLBACK_PENDING, PRODUCT_VALIDATION_FAIL    },
{ PRODUCT_VALIDATION_SERVICE, FAIL,             FINISH_FAIL                },
{ PRODUCT_VALIDATION_SERVICE, SUCCESS,          PAYMENT_SUCCESS            },
{ PAYMENT_SERVICE,            ROLLBACK_PENDING, PAYMENT_FAIL               },
{ PAYMENT_SERVICE,            FAIL,             PRODUCT_VALIDATION_FAIL    },
{ PAYMENT_SERVICE,            SUCCESS,          INVENTORY_SUCCESS          },
{ INVENTORY_SERVICE,          ROLLBACK_PENDING, INVENTORY_FAIL             },
{ INVENTORY_SERVICE,          FAIL,             PAYMENT_FAIL               },
{ INVENTORY_SERVICE,          SUCCESS,          FINISH_SUCCESS             },
```

O `SagaExecutionController` percorre essa matriz com um stream, localiza a linha correspondente ao evento recebido e retorna o próximo tópico para publicação.

## Tecnologias

- Java 17 + Spring Boot
- Apache Kafka (mensageria entre os serviços)
- MongoDB (pedidos e histórico de eventos  order-service)
- PostgreSQL (payment-service, product-validation-service, inventory-service)
- Docker e Docker Compose
- Redpanda Console (UI para visualização dos tópicos Kafka)

## Pré-requisitos

- Docker e Docker Compose instalados
- Portas livres: `3000`, `8080`, `8090`, `8091`, `8092`, `8081`, `9092`, `27017`, `5432`, `5433`, `5434`

## Como executar

Clone o repositório:

```bash
git clone https://github.com/seu-usuario/ms-saga-orchestrator.git
cd ms-saga-orchestrator
```

Suba o ambiente completo:

```bash
docker-compose up --build -d
```

Isso inicializa o Kafka, o Zookeeper, o MongoDB, três instâncias de PostgreSQL, o Redpanda Console e os cinco microsserviços.

Verifique se os containers estão ativos:

```bash
docker ps
```

Os containers esperados são: `order-service`, `orchestrator-service`, `product-validation-service`, `payment-service`, `inventory-service`, `kafka`, `order-db`, `product-db`, `payment-db`, `inventory-db` e `redpanda`.

O Redpanda Console fica disponível em http://localhost:8081 e permite acompanhar os tópicos e mensagens em tempo real.

## Como iniciar uma SAGA

Envie um `POST` para o order-service com a lista de produtos do pedido:

```bash
curl -X POST http://localhost:3000/api/order \
  -H "Content-Type: application/json" \
  -d '{
    "orderProducts": [
      {
        "product": { "code": "COMIC_BOOKS", "unitValue": 15.50 },
        "quantity": 3
      },
      {
        "product": { "code": "NOVELS", "unitValue": 9.99 },
        "quantity": 2
      }
    ]
  }'
```

A partir daí o fluxo é totalmente assíncrono. O order-service publica no tópico `start-saga`, o orquestrador assume o controle e roteia os eventos até o `finish-success` ou `finish-fail`, notificando o order-service ao final pelo tópico `notify-ending`.

Para acompanhar o roteamento em tempo real:

```bash
docker logs orchestrator-service -f
```

## Consultando o histórico de um pedido

```bash
GET http://localhost:3000/api/event?orderId={orderId}
```

## Encerrando o ambiente

```bash
docker-compose down
```

Para remover também os volumes de dados:

```bash
docker-compose down -v
```

## Estrutura do projeto

```
ms-saga-orchestrator/
├── docker-compose.yml
├── order-service/               # REST API de entrada, persistência MongoDB
├── orchestrator-service/        # Coordenador da SAGA, SagaHandler e SagaExecutionController
├── product-validation-service/  # Validação de produtos e rollback
├── payment-service/             # Processamento de pagamento e rollback
└── inventory-service/           # Atualização de estoque e rollback
```
