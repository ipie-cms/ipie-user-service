package in.gov.ipie.service.user.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.idempotency.IdempotentEventHandler;
import in.gov.ipie.common.events.idempotency.ProcessedEventStore;
import in.gov.ipie.service.user.event.AccountProvisionedEvent;
import in.gov.ipie.service.user.service.UserService;

/**
 * Closes the registration loop: ipie-iam-service has created the Keycloak account, so record its id
 * and move the registration out of {@code PROVISIONING}.
 *
 * <p>That transition is what releases the verification email - {@code UserService#accountProvisioned}
 * publishes {@code USER_REGISTRATION_COMPLETED} rather than {@code completeRegistration} doing it,
 * so the link in that email never reaches a user before the account it points at exists.
 *
 * <p>Idempotent on the event id: delivery is at-least-once, and a redelivery must not move a user
 * who has since been verified back to {@code UNVERIFIED} (see {@code User#accountProvisioned}).
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
class AccountProvisionedEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AccountProvisionedEventConsumer.class);

    private final UserService userService;
    private final ProcessedEventStore processedEventStore;

    AccountProvisionedEventConsumer(UserService userService, ProcessedEventStore processedEventStore) {
        this.userService = userService;
        this.processedEventStore = processedEventStore;
    }

    @RabbitListener(queues = "${ipie.integrations.iam-service.rabbitmq.account-provisioned-queue:"
            + "ipie-user-service.events.account-provisioned}")
    void onAccountProvisioned(EventEnvelope<AccountProvisionedEvent> event) {
        IdempotentEventHandler.handle(event.eventId(), processedEventStore, () -> {
            AccountProvisionedEvent payload = event.data();
            LOG.info("Keycloak account {} provisioned for user {}", payload.keycloakUserId(), payload.userId());
            userService.accountProvisioned(payload.userId(), payload.keycloakUserId());
        });
    }
}
