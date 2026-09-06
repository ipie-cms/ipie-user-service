package in.gov.ipie.service.user.messaging;

import org.springframework.kafka.core.KafkaTemplate;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.publisher.EventPublisher;

/**
 * Concrete Kafka binding for the {@link EventPublisher} port (master standards doc, section 9).
 * Keys each record by {@code event.data()} (the affected entity's id, by this template's own
 * convention - see {@code UserService.enqueueEvent}) so every event about the same entity lands on
 * the same partition and consumers see them in order; falls back to {@code eventId} only if the
 * payload is {@code null}. Only ever invoked by {@code OutboxRelayScheduler}, never directly by
 * business code (master standards doc, section 9 - the outbox pattern).
 */
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;
    private final String topic;

    public KafkaEventPublisher(KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(EventEnvelope<?> event) {
        String key = event.data() != null ? event.data().toString() : event.eventId();
        kafkaTemplate.send(topic, key, event);
    }
}

