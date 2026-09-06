package in.gov.ipie.service.user.consent;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Top-level on purpose: Spring Data does not create a proxy for a repository interface nested inside
 * another repository interface, and the failure is at startup with "no qualifying bean", not at
 * compile time.
 */
public interface ConsentNoticeRepository extends JpaRepository<ConsentNoticeJpaEntity, UUID> {

    /**
     * The version in force for a notice code.
     *
     * <p>Highest version rather than newest {@code effective_from}, so a version inserted ahead of
     * its effective date cannot silently become the one people are recorded as having agreed to.
     */
    @Query("select n from ConsentNoticeJpaEntity n where n.code = :code order by n.version desc limit 1")
    Optional<ConsentNoticeJpaEntity> findCurrent(String code);
}
