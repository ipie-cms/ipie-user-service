package in.gov.ipie.service.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.outbox.OutboxStore;
import in.gov.ipie.common.observability.correlation.LoggingContext;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.event.UserEventType;

/**
 * Wraps every event this service emits in the envelope the platform expects, and writes it to the
 * outbox.
 *
 * <p>Extracted from {@code UserServiceImpl}, which held {@link OutboxStore} and the service name
 * purely to build that envelope. Two collaborators that were only ever used together, in one
 * two-line method, are one collaborator.
 *
 * <p>Events go through the outbox and never straight to a publisher: the outbox row is written
 * inside the caller's {@code @Transactional} boundary, which is what makes the entity save and the
 * event atomic (master standards doc, section 9). {@code OutboxRelayScheduler} is the only thing
 * that talks to the real broker.
 */
@Component
public class UserEventPublisher {

    private final OutboxStore outboxStore;

    /** Stamped on every envelope as its source, so a consumer can tell which service emitted it. */
    private final String serviceName;

    public UserEventPublisher(OutboxStore outboxStore, @Value("${spring.application.name}") String serviceName) {
        this.outboxStore = outboxStore;
        this.serviceName = serviceName;
    }

    /** Publishes an event carrying only the user's id - the common case for lifecycle changes. */
    public void publish(UserEventType eventType, User user) {
        publish(eventType, user.getId());
    }

    /** Publishes an event carrying an explicit payload. */
    public void publish(UserEventType eventType, Object payload) {
        EventEnvelope<Object> event = EventEnvelope.create(
                eventType.name(), UserEventType.CONTRACT_VERSION, serviceName,
                LoggingContext.correlationId(), null, payload);
        outboxStore.save(event);
    }
}
