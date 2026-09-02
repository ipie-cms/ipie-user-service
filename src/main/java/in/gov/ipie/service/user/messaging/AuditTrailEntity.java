package in.gov.ipie.service.user.messaging;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import in.gov.ipie.common.audit.model.AuditEvent;

/**
 * The queryable, persisted counterpart to every {@code AuditEvent} this service records -
 * {@code AuditRecorder}/{@code OutboxAuditRecorder} only guarantee durable delivery into the
 * outbox/broker pipeline (see that class's own Javadoc); this table, written by
 * {@link RabbitUserEventLogConsumer}, is what makes the trail queryable. {@code correlationId} is
 * the connecting column - the same id a single business action carries across every service and
 * every audit row it produces, including in other services (see {@code
 * PillarResolutionEventConsumer} in ipie-iam-service for the cross-service case).
 */
@Entity
@Table(name = "audit_trail")
class AuditTrailEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", length = 100)
    private String entityId;

    @Column(name = "case_id", length = 100)
    private String caseId;

    @Column(name = "actor_user_id", length = 100)
    private String actorUserId;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "old_value")
    private String oldValue;

    @Column(name = "new_value")
    private String newValue;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "persisted_at", nullable = false)
    private Instant persistedAt;

    protected AuditTrailEntity() {
        // required by JPA
    }

    /**
     * Takes the already-bundled {@link AuditEvent} itself (rather than one parameter per field,
     * which would trip Checkstyle's {@code ParameterNumber} limit for no real benefit) plus the
     * two fields the event's own {@code oldValue}/{@code newValue} need pre-serialized to before
     * they can go in a TEXT column - {@code AuditEvent} carries them as {@code Object} (whatever
     * SpEL evaluated), this table stores their already-serialized JSON form.
     */
    AuditTrailEntity(UUID id, AuditEvent event, String oldValueJson, String newValueJson, Instant persistedAt) {
        this.id = id;
        this.eventType = event.eventType().name();
        this.action = event.action();
        this.entityType = event.entityType();
        this.entityId = event.entityId();
        this.caseId = event.caseId();
        this.actorUserId = event.actorUserId();
        this.sourceIp = event.sourceIp();
        this.serviceName = event.serviceName();
        this.comment = event.comment();
        this.oldValue = oldValueJson;
        this.newValue = newValueJson;
        this.correlationId = event.correlationId();
        this.occurredAt = event.occurredAt();
        this.persistedAt = persistedAt;
    }
}
