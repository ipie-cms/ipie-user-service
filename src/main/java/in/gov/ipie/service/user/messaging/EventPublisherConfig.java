package in.gov.ipie.service.user.messaging;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.publisher.EventPublisher;

/**
 * Chooses the {@link EventPublisher} implementation, in order of precedence: {@link
 * KafkaEventPublisher} when a Kafka broker is configured, {@link RabbitEventPublisher} when
 * RabbitMQ is configured instead (standby in case Kafka doesn't get organizational clearance -
 * see {@code docker-compose.yml}'s {@code rabbitmq} service), {@link LoggingEventPublisher}
 * otherwise. The two real bindings are mutually exclusive by design - {@code
 * @ConditionalOnMissingBean(EventPublisher.class)} on the RabbitMQ bean means Kafka always wins if
 * both happen to be configured at once, which should never happen in practice.
 *
 * <p>The Kafka binding is built as an explicit {@code ProducerFactory} (rather than relying on
 * Spring Boot's autoconfigured {@code KafkaTemplate<Object, Object>}) so this class controls the
 * exact generic type end to end - avoids an autowiring mismatch between the generically-typed bean
 * Spring Boot would otherwise create and what {@link KafkaEventPublisher} actually needs. RabbitMQ
 * has no equivalent generic-typing concern, so its {@link RabbitTemplate} is built directly from
 * Spring Boot's autoconfigured {@link ConnectionFactory}.
 */
@Configuration
public class EventPublisherConfig {

    @Bean
    @ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
    public EventPublisher kafkaEventPublisher(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${ipie.events.kafka.topic}") String topic) {
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        ProducerFactory<String, EventEnvelope<?>> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        return new KafkaEventPublisher(new KafkaTemplate<>(producerFactory), topic);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher rabbitEventPublisher(
            ConnectionFactory connectionFactory,
            @Value("${ipie.events.rabbitmq.exchange}") String exchange) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return new RabbitEventPublisher(rabbitTemplate, exchange);
    }

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher loggingEventPublisher(ObjectMapper objectMapper) {
        return new LoggingEventPublisher(objectMapper);
    }
}

