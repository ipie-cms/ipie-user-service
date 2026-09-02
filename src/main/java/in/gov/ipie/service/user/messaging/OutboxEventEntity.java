package in.gov.ipie.service.user.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import in.gov.ipie.common.events.jpa.AbstractOutboxEventEntity;

/**
 * Where this service's outbox rows live. The row shape and every query over it belong to the
 * platform ({@link AbstractOutboxEventEntity}); the table name is the one thing that is genuinely
 * this service's own.
 */
@Entity
@Table(name = "outbox_events")
class OutboxEventEntity extends AbstractOutboxEventEntity {
}
