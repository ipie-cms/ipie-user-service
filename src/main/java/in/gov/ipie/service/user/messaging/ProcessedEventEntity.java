package in.gov.ipie.service.user.messaging;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import in.gov.ipie.common.events.jpa.AbstractProcessedEventEntity;

/**
 * Where this service records the event ids it has already acted on, so a redelivery does not act
 * twice. Shape and behaviour are the platform's; only the table name is this service's.
 */
@Entity
@Table(name = "processed_events")
class ProcessedEventEntity extends AbstractProcessedEventEntity {
}
