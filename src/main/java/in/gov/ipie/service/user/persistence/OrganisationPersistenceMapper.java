package in.gov.ipie.service.user.persistence;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.service.user.domain.Organisation;

/**
 * Converts between the JPA entity and the domain model - same reasoning as {@code
 * UserPersistenceMapper}, whose shape this mirrors.
 */
@Component
public class OrganisationPersistenceMapper {

    public Organisation toDomain(OrganisationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        AuditMetadata auditMetadata = new AuditMetadata(
                entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getVersion(),
                entity.isActive(), entity.getDeletedAt(), entity.getDeletedBy());
        return Organisation.builder()
                .id(entity.getId())
                .name(entity.getName())
                .legalConstitution(entity.getLegalConstitution())
                .idType(entity.getIdType())
                .idValue(entity.getIdValue())
                .msme(entity.isMsme())
                .msmeType(entity.getMsmeType())
                .registeredAddress(entity.getRegisteredAddress())
                .contactNumber(entity.getContactNumber())
                .contactEmail(entity.getContactEmail())
                .country(entity.getCountry())
                .state(entity.getState())
                .city(entity.getCity())
                .pin(entity.getPin())
                .district(entity.getDistrict())
                .auditMetadata(auditMetadata)
                .build();
    }

    public OrganisationJpaEntity toNewEntity(Organisation organisation) {
        OrganisationJpaEntity entity = new OrganisationJpaEntity(
                organisation.getId(), organisation.getName(), organisation.getLegalConstitution(),
                organisation.getIdType(), organisation.getIdValue());
        copyMutableFieldsOnto(organisation, entity);
        return entity;
    }

    public void copyMutableFieldsOnto(Organisation organisation, OrganisationJpaEntity entity) {
        entity.setName(organisation.getName());
        entity.setLegalConstitution(organisation.getLegalConstitution());
        entity.setMsme(organisation.isMsme());
        entity.setMsmeType(organisation.getMsmeType());
        entity.setRegisteredAddress(organisation.getRegisteredAddress());
        entity.setContactNumber(organisation.getContactNumber());
        entity.setContactEmail(organisation.getContactEmail());
        entity.setCountry(organisation.getCountry());
        entity.setState(organisation.getState());
        entity.setCity(organisation.getCity());
        entity.setPin(organisation.getPin());
        entity.setDistrict(organisation.getDistrict());
    }
}
