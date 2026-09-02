package in.gov.ipie.service.user.messaging;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.utils.id.IdGenerator;
import in.gov.ipie.common.audit.model.AuditEvent;
import in.gov.ipie.common.audit.outbox.AuditEventCodec;
import in.gov.ipie.common.audit.outbox.AuditValueMasker;
import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;

/**
 * Reference consumer for this service's own {@code ipie-user-service.events} topic -
 * demonstrates the required consumer-side idempotency pattern (master standards doc, section 9:
 * "Consumers must handle duplicate delivery") via {@link IdempotentEventHandler}, the same way
 * {@code KafkaEventPublisher} demonstrates the producer side. {@code @ConditionalOnProperty} at
 * the class level - not just on {@code EventConsumerConfig}'s factory bean - is essential: Spring
 * Boot's own Kafka auto-configuration provides a default listener container factory whenever
 * {@code spring-kafka} is on the classpath, regardless of whether {@code EventConsumerConfig}'s
 * conditional factory fires, so an unconditional {@code @Component} here would still bind and
 * attempt a connection (to Spring Boot's own default, {@code localhost:9092}) even when Kafka
 * isn't the configured broker.
 *
 * <p>Also persists the queryable audit trail, the Kafka counterpart to {@code
 * RabbitUserEventLogConsumer}'s own persistence - every event this service publishes flows
 * through this one topic, {@code AUDIT_EVENT} envelopes included (see {@code
 * OutboxAuditRecorder}), so this is the extension point - not a second, parallel listener - for
 * turning "reliably delivered to the broker" into "queryable in {@code audit_trail}" regardless
 * of which broker is actually configured. See {@link AuditEventCodec}'s Javadoc.
 */
@Component
@ConditionalOnProperty(prefix = "spring.kafka", name = "bootstrap-servers")
class UserEventLogConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(UserEventLogConsumer.class);

    private final ProcessedEventStore processedEventStore;
    private final AuditTrailJpaRepository auditTrailRepository;
    private final ObjectMapper objectMapper;

    UserEventLogConsumer(
            ProcessedEventStore processedEventStore, AuditTrailJpaRepository auditTrailRepository, ObjectMapper objectMapper) {
        this.processedEventStore = processedEventStore;
        this.auditTrailRepository = auditTrailRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${ipie.events.kafka.topic}", groupId = "${spring.application.name}.user-event-log")
    void onEvent(EventEnvelope<?> event) {
        IdempotentEventHandler.handle(event.eventId(), processedEventStore, () -> {
            LOG.info("Processed event [{} v{}] {} for entity {}",
                    event.eventType(), event.eventVersion(), event.eventId(), event.data());
            persistIfAuditEvent(event);
        });
    }

    private void persistIfAuditEvent(EventEnvelope<?> event) {
        Optional<AuditEvent> auditEvent = AuditEventCodec.decodeIfAuditEvent(event, objectMapper);
        if (auditEvent.isEmpty()) {
            return;
        }
        AuditEvent decoded = auditEvent.get();
        auditTrailRepository.save(new AuditTrailEntity(
                IdGenerator.newUuid(), decoded, toJson(decoded.oldValue()), toJson(decoded.newValue()), Instant.now()));
    }

    /** Delegates to {@link AuditValueMasker} - known-PII field names (email, phone, ...) are masked before storage. */
    private String toJson(Object value) {
        try {
            return AuditValueMasker.maskAndSerialize(value, objectMapper);
        } catch (IllegalStateException e) {
            LOG.warn("Failed to serialize an audit-trail old/new value snapshot, storing as null", e);
            return null;
        }
    }
}
