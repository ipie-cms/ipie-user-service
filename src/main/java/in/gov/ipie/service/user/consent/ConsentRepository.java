package in.gov.ipie.service.user.consent;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<UserConsentJpaEntity, UUID> {

    /** The items this person is consenting to right now - withdrawn rows are history, not state. */
    List<UserConsentJpaEntity> findByUserIdAndWithdrawnAtIsNull(UUID userId);
}
