package in.gov.ipie.service.user.persistence;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.service.user.domain.PillarLink;
import in.gov.ipie.service.user.domain.PillarLinkRequest;

/** Hand-written mapper (assembles {@link AuditMetadata}) - see {@code UserPersistenceMapper}'s Javadoc for the convention. */
@Component
public class PillarLinkPersistenceMapper {

    public PillarLink toDomain(PillarLinkJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        AuditMetadata auditMetadata = new AuditMetadata(
                entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getVersion(),
                entity.isActive(), entity.getDeletedAt(), entity.getDeletedBy());
        return PillarLink.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .pillarType(entity.getPillarType())
                .externalPillarId(entity.getExternalPillarId())
                .externalUsername(entity.getExternalUsername())
                .linkedAt(entity.getLinkedAt())
                .auditMetadata(auditMetadata)
                .build();
    }

    public PillarLinkJpaEntity toNewEntity(PillarLink link) {
        return new PillarLinkJpaEntity(
                link.getUserId(), link.getPillarType(), link.getExternalPillarId(),
                link.getExternalUsername(), link.getLinkedAt());
    }

    public PillarLinkRequest toDomain(PillarLinkRequestJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        AuditMetadata auditMetadata = new AuditMetadata(
                entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getVersion(),
                entity.isActive(), entity.getDeletedAt(), entity.getDeletedBy());
        return PillarLinkRequest.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .pillarType(entity.getPillarType())
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .auditMetadata(auditMetadata)
                .build();
    }

    public PillarLinkRequestJpaEntity toNewEntity(PillarLinkRequest request) {
        return new PillarLinkRequestJpaEntity(
                request.getUserId(), request.getPillarType(), request.getStatus(), request.getExpiresAt());
    }

    public void copyMutableFieldsOnto(PillarLinkRequest request, PillarLinkRequestJpaEntity entity) {
        entity.setStatus(request.getStatus());
    }
}
