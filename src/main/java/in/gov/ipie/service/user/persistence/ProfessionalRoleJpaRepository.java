package in.gov.ipie.service.user.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalRoleJpaRepository extends JpaRepository<ProfessionalRoleJpaEntity, UUID> {

    List<ProfessionalRoleJpaEntity> findByActiveTrueOrderBySortOrderAsc();
}
