package in.gov.ipie.service.user.messaging;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.utils.id.IdGenerator;
import in.gov.ipie.common.audit.model.AuditEvent;
import in.gov.ipie.common.audit.outbox.AuditEventCodec;
import in.gov.ipie.common.audit.outbox.AuditValueMasker;
import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;

/**
 * RabbitMQ counterpart to {@code UserEventLogConsumer} - demonstrates the same required
 * consumer-side idempotency pattern (master standards doc, section 9) when this service is
 * configured to use RabbitMQ instead of Kafka (standby in case Kafka doesn't get organizational
 * clearance). {@code @ConditionalOnProperty} at the class level - not just on the listener method
 * - is essential here: Spring Boot's own RabbitMQ auto-configuration provides a default listener
 * container factory whenever {@code spring-boot-starter-amqp} is on the classpath, regardless of
 * whether {@code RabbitConsumerConfig}'s conditional factory fires, so an unconditional
 * {@code @Component} would still bind and attempt a connection even when RabbitMQ isn't the
 * configured broker.
 *
 * <p>Also persists the queryable audit trail: every event this service publishes flows through
 * this one queue, {@code AUDIT_EVENT} envelopes included (see {@code OutboxAuditRecorder}), so
 * this is the extension point - not a second, parallel listener - for turning "reliably delivered
 * to the broker" into "queryable in {@code audit_trail}." See {@link AuditEventCodec}'s Javadoc.
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
class RabbitUserEventLogConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitUserEventLogConsumer.class);

    private final ProcessedEventStore processedEventStore;
    private final AuditTrailJpaRepository auditTrailRepository;
    private final ObjectMapper objectMapper;

    RabbitUserEventLogConsumer(
            ProcessedEventStore processedEventStore, AuditTrailJpaRepository auditTrailRepository, ObjectMapper objectMapper) {
        this.processedEventStore = processedEventStore;
        this.auditTrailRepository = auditTrailRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${ipie.events.rabbitmq.queue}")
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

