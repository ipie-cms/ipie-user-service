package in.gov.ipie.service.user.messaging;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AuditTrailJpaRepository extends JpaRepository<AuditTrailEntity, UUID> {
}
