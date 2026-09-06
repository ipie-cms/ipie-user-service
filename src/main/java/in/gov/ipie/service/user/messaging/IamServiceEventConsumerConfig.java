package in.gov.ipie.service.user.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.gov.ipie.common.events.deadletter.DeadLetterSupport;

/**
 * Binds this service to ipie-iam-service's exchange, for the reply leg of asynchronous account
 * provisioning - the mirror of the {@code UserServiceEventConsumerConfig} that ipie-iam-service uses
 * to listen to this service.
 *
 * <p>Declaring the other service's exchange here is safe and intentional: an AMQP exchange
 * declaration is idempotent, and whichever side starts first creates it. Without it, a
 * user-service that boots before ipie-iam-service has ever run would have nothing to bind its queue
 * to.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
public class IamServiceEventConsumerConfig {

    @Bean
    public TopicExchange iamServiceEventsExchange(
            @Value("${ipie.integrations.iam-service.rabbitmq.exchange:ipie-iam-service.events}") String exchange) {
        return new TopicExchange(exchange);
    }

    @Bean
    public Queue accountProvisionedQueue(
            @Value("${ipie.integrations.iam-service.rabbitmq.account-provisioned-queue:"
                    + "ipie-user-service.events.account-provisioned}") String queue) {
        return DeadLetterSupport.workQueue(queue);
    }

    @Bean
    public Binding accountProvisionedBinding(Queue accountProvisionedQueue, TopicExchange iamServiceEventsExchange) {
        return BindingBuilder.bind(accountProvisionedQueue).to(iamServiceEventsExchange).with("ACCOUNT_PROVISIONED");
    }
}
