import os
import platform
import subprocess
import threading
import shutil

SERVICES = [
    "order-service",
    "orchestrator-service",
    "product-validation-service",
    "payment-service",
    "inventory-service",
]

IS_WINDOWS = platform.system() == "Windows"
GRADLEW = "gradlew.bat" if IS_WINDOWS else "./gradlew"

# Compose V2 (preferido); se não existir, cai para docker-compose (V1)
COMPOSE_CMD = "docker compose" if shutil.which("docker") else "docker-compose"
if " " in COMPOSE_CMD:  # 'docker compose' precisa shell=True
    COMPOSE_BASE = COMPOSE_CMD
else:
    COMPOSE_BASE = COMPOSE_CMD

def run(cmd: str) -> int:
    print(f">>> {cmd}")
    # shell=True porque usamos 'docker compose' (com espaço) e porque é script utilitário
    return subprocess.call(cmd, shell=True)

def ensure_gradle_wrapper():
    # Garante que o wrapper existe na raiz (gradlew / gradle/wrapper/*)
    missing = []
    if not os.path.exists(GRADLEW):
        missing.append(GRADLEW)
    if not os.path.exists(os.path.join("gradle", "wrapper", "gradle-wrapper.jar")):
        missing.append("gradle/wrapper/gradle-wrapper.jar")
    if missing:
        print(f"[INFO] Wrapper ausente ({missing}). Gerando wrapper do Gradle…")
        # Tenta gerar com o gradle global; se não tiver, falha com mensagem clara
        if shutil.which("gradle"):
            rc = run("gradle wrapper --gradle-version 8.10.2")
            if rc != 0:
                raise SystemExit("[ERRO] Não foi possível gerar o wrapper com 'gradle wrapper'.")
        else:
            raise SystemExit("[ERRO] gradlew/gradle-wrapper não encontrados e 'gradle' global não está disponível.")

    # Permissão no Unix
    if not IS_WINDOWS:
        os.chmod(GRADLEW, 0o755)

def build_service(service: str):
    print(f"Building {service} …")
    # Use o wrapper da RAIZ + task QUALIFICADA do subprojeto; evita 'cd' e evita usar gradle global
    # Se for Spring Boot: bootJar; se não, troque por 'build'
    rc = run(f"{GRADLEW} :{service}:bootJar -x test")
    if rc != 0:
        print(f"[FAIL] {service} build retornou código {rc}")
    else:
        print(f"[OK] {service} build")

def build_all():
    print("Starting to build applications!")
    threads = []
    for svc in SERVICES:
        t = threading.Thread(target=build_service, args=(svc,))
        t.start()
        threads.append(t)
    for t in threads:
        t.join()

def docker_ready() -> bool:
    # Checa se o Docker Desktop/engine responde
    return run("docker version >nul 2>&1" if IS_WINDOWS else "docker version >/dev/null 2>&1") == 0

def compose_down():
    print("Removing all containers.")
    # Remove containers, redes e volumes do compose do projeto atual
    # --remove-orphans ajuda quando nomes mudam
    run(f"{COMPOSE_BASE} down -v --remove-orphans")

def compose_up():
    print("Running containers!")
    # build + up (detached)
    run(f"{COMPOSE_BASE} up -d --build")
    print("Pipeline finished!")

if __name__ == "__main__":
    print("Pipeline started!")
    ensure_gradle_wrapper()
    # Dica: se quiser forçar a JDK só para o Gradle, descomente e ajuste:
    # os.environ["GRADLE_JAVA_HOME"] = r"C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"

    build_all()

    if docker_ready():
        compose_down()
        compose_up()
    else:
        print("[WARN] Docker Desktop/engine não está pronto. Abra o Docker Desktop (contexto 'desktop-linux') e tente novamente.")
        print("       Dicas: 'docker context ls', 'docker context use desktop-linux', 'docker ps'.")
