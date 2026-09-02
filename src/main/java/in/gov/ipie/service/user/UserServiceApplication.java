package in.gov.ipie.service.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Owns the user registration lifecycle (PRE_REGISTRATION -&gt; UNVERIFIED -&gt; VERIFIED) and
 * provisions the corresponding Keycloak account - see {@code
 * application.service.RegistrationService}.
 *
 * <p>{@code @EnableScheduling} drives {@code OutboxRelayScheduler} - the transactional outbox
 * relay (master standards doc, section 9) - not any business-specific scheduled job.
 *
 * <p>{@code @ConfigurationPropertiesScan} picks up {@code PillarLinkingProperties} (the
 * per-pillar-type OIDC config for pillar-account linking) - every other config value in
 * this service is a handful of {@code @Value}-injected scalars, but 4 pillar types x 5
 * properties each doesn't fit that convention.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
