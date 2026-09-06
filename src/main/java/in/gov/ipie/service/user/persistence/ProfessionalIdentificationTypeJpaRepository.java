package in.gov.ipie.service.user.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalIdentificationTypeJpaRepository extends JpaRepository<ProfessionalIdentificationTypeJpaEntity, UUID> {

    List<ProfessionalIdentificationTypeJpaEntity> findByActiveTrueOrderBySortOrderAsc();
}
