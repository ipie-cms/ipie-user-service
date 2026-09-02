package in.gov.ipie.service.user.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import in.gov.ipie.common.events.deadletter.DeadLetterSupport;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Consumer-side RabbitMQ wiring for {@code messaging.consumer.RabbitUserEventLogConsumer} - only
 * activates when {@code spring.rabbitmq.host} is configured, mirroring {@code
 * EventConsumerConfig}'s condition for the Kafka side, so unit/integration tests that never set it
 * do not attempt any RabbitMQ connection at all. Declares the topic exchange, the queue this
 * service's own consumer reads from, and the binding between them - RabbitMQ needs this topology
 * declared explicitly, unlike Kafka where a topic is created implicitly on first use.
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
public class RabbitConsumerConfig {

    @Bean
    public TopicExchange ipieEventsExchange(@Value("${ipie.events.rabbitmq.exchange}") String exchange) {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue userEventLogQueue(@Value("${ipie.events.rabbitmq.queue}") String queue) {
        return DeadLetterSupport.workQueue(queue);
    }

    /**
     * Binds the queue to every routing key on the exchange ({@code #}) - this reference consumer
     * logs every event type this service publishes, the RabbitMQ equivalent of {@code
     * UserEventLogConsumer} subscribing to the whole Kafka topic.
     */
    @Bean
    public Binding userEventLogBinding(Queue userEventLogQueue, TopicExchange ipieEventsExchange) {
        return BindingBuilder.bind(userEventLogQueue).to(ipieEventsExchange).with("#");
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // Apply Spring Boot's spring.rabbitmq.listener.* first (see ipie-events-defaults.yml in
        // common-libs). Building the factory by hand skips them entirely, and the two that matter
        // here are exactly the ones that make dead-lettering work: bounded retries, and
        // default-requeue-rejected=false. Without them a message the consumer cannot handle is
        // requeued immediately and forever - the consumer spins on it and nothing reaches the DLQ.
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        return factory;
    }
}

