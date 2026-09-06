package in.gov.ipie.service.user.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalRepresentativeTypeJpaRepository extends JpaRepository<LegalRepresentativeTypeJpaEntity, UUID> {

    List<LegalRepresentativeTypeJpaEntity> findByActiveTrueOrderBySortOrderAsc();
}
