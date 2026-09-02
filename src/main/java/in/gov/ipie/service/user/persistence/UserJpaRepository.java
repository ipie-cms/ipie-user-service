package in.gov.ipie.service.user.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


/**
 * Public so {@code UserRepositoryImpl} (the {@code repositoryimpl} sibling subpackage) can use
 * it - by convention, no other class should reference this interface.
 */
public interface UserJpaRepository
        extends JpaRepository<UserJpaEntity, UUID>, JpaSpecificationExecutor<UserJpaEntity>, UserJpaRepositoryCustom {

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID excludedId);

    /**
     * Named for the association, not the column: the phone number moved to `person` in V13, and
     * Spring Data resolves `PersonPhoneNumber` as a walk through the person association rather than
     * a property of `users` that no longer exists.
     */
    boolean existsByPersonPhoneNumber(String phoneNumber);

    Optional<UserJpaEntity> findByVerificationTokenHash(String verificationTokenHash);

    Optional<UserJpaEntity> findByKeycloakUserId(UUID keycloakUserId);
}

