package in.gov.ipie.service.user.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.publisher.EventPublisher;

/**
 * Fallback {@link EventPublisher}: writes each envelope as a structured JSON log line under the
 * {@code EVENTS} logger instead of publishing anywhere. Registered by {@code EventPublisherConfig}
 * only when no Kafka broker is configured (e.g. a bare {@code java -jar} run with no
 * {@code spring.kafka.bootstrap-servers}) - only {@code OutboxRelayScheduler} depends on the
 * {@link EventPublisher} port directly (business code writes to the outbox, never to this port),
 * so it never needs to know which implementation is active.
 */
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger EVENTS_LOG = LoggerFactory.getLogger("EVENTS");

    private final ObjectMapper objectMapper;

    public LoggingEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(EventEnvelope<?> event) {
        try {
            EVENTS_LOG.info(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            EVENTS_LOG.error("Failed to serialize event {} ({})", event.eventId(), event.eventType(), e);
        }
    }
}

