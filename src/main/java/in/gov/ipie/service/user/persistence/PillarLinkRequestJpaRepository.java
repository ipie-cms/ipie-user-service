package in.gov.ipie.service.user.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;


/** Public so {@code PillarLinkRequestRepositoryImpl} (the {@code repositoryimpl} sibling subpackage) can use it. */
public interface PillarLinkRequestJpaRepository extends JpaRepository<PillarLinkRequestJpaEntity, UUID> {
}
