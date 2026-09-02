package in.gov.ipie.service.user.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.common.events.jpa.JpaOutboxStore;
import in.gov.ipie.common.events.jpa.JpaProcessedEventStore;
import in.gov.ipie.common.events.outbox.OutboxStore;

/**
 * Points the platform's event stores at this service's two tables.
 *
 * <p>This replaced about a hundred and ninety lines that were byte-identical in every service - the
 * stores, their Spring Data repositories and the row shapes. The platform owns the port and now
 * ships its one implementation, so a change to either reaches every service through a version bump
 * rather than through four edits that have to be remembered.
 *
 * <p>The concrete entity type and its constructor are passed in because the entity is declared here:
 * only this service knows which table it writes to. A method reference rather than reflection, so an
 * entity that cannot be constructed fails the compile instead of the first event published.
 */
@Configuration
class EventStoreConfig {

    @Bean
    OutboxStore outboxStore(EntityManager entityManager, ObjectMapper objectMapper) {
        return new JpaOutboxStore<>(entityManager, objectMapper, OutboxEventEntity.class, OutboxEventEntity::new);
    }

    @Bean
    ProcessedEventStore processedEventStore(EntityManager entityManager) {
        return new JpaProcessedEventStore<>(entityManager, ProcessedEventEntity.class, ProcessedEventEntity::new);
    }
}
