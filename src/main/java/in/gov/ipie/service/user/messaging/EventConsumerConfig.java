package in.gov.ipie.service.user.messaging;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import in.gov.ipie.common.events.envelope.EventEnvelope;

/**
 * Consumer-side Kafka wiring for {@code UserEventLogConsumer} - only activates when a broker is
 * actually configured, mirroring {@link EventPublisherConfig}'s condition on the producer side, so
 * unit/integration tests that never set {@code spring.kafka.bootstrap-servers} do not attempt any
 * Kafka connection at all.
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
public class EventConsumerConfig {

    @Bean
    public ConsumerFactory<String, EventEnvelope<?>> eventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> consumerProps = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                JsonDeserializer.VALUE_DEFAULT_TYPE, EventEnvelope.class,
                JsonDeserializer.TRUSTED_PACKAGES, "in.gov.ipie.common.events.envelope");
        return new DefaultKafkaConsumerFactory<>(consumerProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventEnvelope<?>> kafkaListenerContainerFactory(
            ConsumerFactory<String, EventEnvelope<?>> eventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, EventEnvelope<?>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventConsumerFactory);
        return factory;
    }
}

