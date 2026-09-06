package in.gov.ipie.service.user.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.publisher.EventPublisher;

/**
 * Standby RabbitMQ binding for the {@link EventPublisher} port (master standards doc, section 9)
 * - in case Kafka doesn't get organizational clearance. Routes by {@code event.eventType()} on a
 * topic exchange (see {@code RabbitConsumerConfig}), the RabbitMQ analogue of Kafka's
 * partition-by-key: a consumer binds a queue to the routing keys it cares about instead of
 * subscribing to a whole topic. Only ever invoked by {@code OutboxRelayScheduler}, never directly
 * by business code (master standards doc, section 9 - the outbox pattern), same as
 * {@code KafkaEventPublisher}.
 */
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate, String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publish(EventEnvelope<?> event) {
        rabbitTemplate.convertAndSend(exchange, event.eventType(), event);
    }
}

