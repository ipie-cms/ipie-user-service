package in.gov.ipie.service.user.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.gov.ipie.common.persistence.IntegrityViolations;

/**
 * Publishes this service's constraint declarations so the shared error boundary can read them.
 *
 * <p>Each repository declares the constraints of the table it owns, because only it knows them - but
 * a declaration held as a private constant is only ever consulted by the {@code catch} block beside
 * it, and that block is not reached when Hibernate defers the insert to flush. The violation then
 * surfaces from the transaction commit, after the repository has returned, and only
 * {@code GlobalExceptionHandler} is still on the stack. Registering the same declarations as beans
 * is what lets the boundary translate a deferred violation exactly as the repository would have
 * translated an immediate one.
 */
@Configuration
class IntegrityViolationsConfig {

    @Bean
    IntegrityViolations userIntegrityViolations() {
        return UserRepositoryImpl.VIOLATIONS;
    }

    @Bean
    IntegrityViolations organisationIntegrityViolations() {
        return OrganisationRepositoryImpl.VIOLATIONS;
    }

    @Bean
    IntegrityViolations pillarLinkIntegrityViolations() {
        return PillarLinkRepositoryImpl.VIOLATIONS;
    }

    /**
     * {@code person} has no repository of its own either - person detail is written through the user
     * aggregate (V13), so its constraints are declared beside that aggregate's rather than beside a
     * {@code catch} that does not exist. The phone-number message is the one {@code users} used to
     * carry: the column moved tables, and an API consumer should not be able to tell.
     */
    @Bean
    IntegrityViolations personIntegrityViolations() {
        return IntegrityViolations.forTable()
                .primaryKey("person_pkey")
                .conflict("uq_person_phone_number", "A user with this phone number already exists")
                .conflict("uq_person_user_id", "That principal already has person detail")
                .build();
    }

    /**
     * {@code user_professional_roles} has no repository of its own - it is written through the user
     * aggregate - so its one uniqueness rule is declared here rather than beside a {@code catch}
     * that does not exist. Without it, adding a role a user already holds is an unexpected 500.
     */
    @Bean
    IntegrityViolations userProfessionalRoleIntegrityViolations() {
        return IntegrityViolations.forTable()
                .conflict("uq_user_professional_roles", "That professional role is already recorded for this user")
                .build();
    }
}
