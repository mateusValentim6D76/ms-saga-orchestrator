package br.com.microservices.orchestrated.orchestratorservice.config.kafka;

import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics.FINISH_FAIL;
import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics.FINISH_SUCCESS;

@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    private static final Integer REPLICA_COUNT = 1;

    private static final Integer PARTITION_COUNT = 1;

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerProps());
    }

    private Map<String, Object> consumerProps() {
        var propsConsumer = new HashMap<String, Object>();
        propsConsumer.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        propsConsumer.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        propsConsumer.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        propsConsumer.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsConsumer.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        return propsConsumer;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    private Map<String, Object> producerProps() {
        var propsProducer = new HashMap<String, Object>();
        propsProducer.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        propsProducer.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        propsProducer.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return propsProducer;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory){
        return new KafkaTemplate<>(producerFactory());
    }

    private NewTopic buildTopics(String topicName) {
        return TopicBuilder
                .name(topicName)
                .replicas(REPLICA_COUNT)
                .partitions(PARTITION_COUNT)
                .build();
    }

    @Bean
    public NewTopic startSagaTopic() {
        return buildTopics(ETopics.START_SAGA.getTopics());
    }

    @Bean
    public NewTopic baseOrchestratorTopic() {
        return buildTopics(ETopics.BASE_ORCHESTRATOR.getTopics());
    }

    @Bean
    public NewTopic finishSuccessTopic() {
        return buildTopics(FINISH_SUCCESS.getTopics());
    }

    @Bean
    public NewTopic finishFailTopic() {
        return buildTopics(FINISH_FAIL.getTopics());
    }

    @Bean
    public NewTopic notifyEndingTopic() {
        return buildTopics(ETopics.NOTIFY_ENDINDG.getTopics());
    }

    @Bean
    public NewTopic productValidationSuccessTopic() {
        return buildTopics(ETopics.PRODUCT_VALIDATION_SUCCESS.getTopics());
    }

    @Bean
    public NewTopic productValidationFailTopic() {
        return buildTopics(ETopics.PRODUCT_VALIDATION_FAIL.getTopics());
    }

    @Bean
    public NewTopic paymentSuccessTopic() {
        return buildTopics(ETopics.PAYMENT_SUCCESS.getTopics());
    }

    @Bean
    public NewTopic paymentFailTopic() {
        return buildTopics(ETopics.PAYMENT_FAIL.getTopics());
    }

    @Bean
    public NewTopic inventorySuccessTopic() {
        return buildTopics(ETopics.INVENTORY_SUCCESS.getTopics());
    }

    @Bean
    public NewTopic inventoryFailTopic() {
        return buildTopics(ETopics.INVENTORY_FAIL.getTopics());
    }
}
