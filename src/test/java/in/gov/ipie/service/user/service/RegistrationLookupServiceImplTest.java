package in.gov.ipie.service.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.service.user.domain.LookupOption;
import in.gov.ipie.service.user.persistence.IdentityProofTypeJpaEntity;
import in.gov.ipie.service.user.persistence.IdentityProofTypeJpaRepository;
import in.gov.ipie.service.user.persistence.LegalRepresentativeTypeJpaRepository;
import in.gov.ipie.service.user.persistence.ProfessionalIdentificationTypeJpaRepository;
import in.gov.ipie.service.user.persistence.ProfessionalRoleJpaEntity;
import in.gov.ipie.service.user.persistence.ProfessionalRoleJpaRepository;

/**
 * Only {@link RegistrationLookupServiceImpl#listProfessionalRoles} and {@code
 * #listIdentityProofTypes} are exercised directly - the other two methods are identical in shape
 * (see the class's own Javadoc on why there's no per-catalogue domain/repository split to test
 * separately).
 */
class RegistrationLookupServiceImplTest {

    private final ProfessionalRoleJpaRepository professionalRoleRepository = mock(ProfessionalRoleJpaRepository.class);
    private final LegalRepresentativeTypeJpaRepository legalRepresentativeTypeRepository =
            mock(LegalRepresentativeTypeJpaRepository.class);
    private final ProfessionalIdentificationTypeJpaRepository professionalIdentificationTypeRepository =
            mock(ProfessionalIdentificationTypeJpaRepository.class);
    private final IdentityProofTypeJpaRepository identityProofTypeRepository = mock(IdentityProofTypeJpaRepository.class);

    private final RegistrationLookupServiceImpl lookupService = new RegistrationLookupServiceImpl(
            professionalRoleRepository, legalRepresentativeTypeRepository, professionalIdentificationTypeRepository,
            identityProofTypeRepository);

    @Test
    void listProfessionalRoles_mapsActiveRowsInSortOrder() throws Exception {
        UUID id = UUID.randomUUID();
        ProfessionalRoleJpaEntity entity = newLookupEntity(ProfessionalRoleJpaEntity.class, id, "INSOLVENCY_PROFESSIONAL",
                "Insolvency Professional");
        when(professionalRoleRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(entity));

        List<LookupOption> result = lookupService.listProfessionalRoles();

        assertThat(result).containsExactly(new LookupOption(id, "INSOLVENCY_PROFESSIONAL", "Insolvency Professional"));
    }

    @Test
    void listIdentityProofTypes_mapsActiveRowsInSortOrder() throws Exception {
        UUID id = UUID.randomUUID();
        IdentityProofTypeJpaEntity entity = newLookupEntity(IdentityProofTypeJpaEntity.class, id, "PAN", "PAN Card");
        when(identityProofTypeRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(entity));

        List<LookupOption> result = lookupService.listIdentityProofTypes();

        assertThat(result).containsExactly(new LookupOption(id, "PAN", "PAN Card"));
    }

    /** These JPA entities have no public constructor/builder (rows only ever come from Hibernate) - reflection stands in for that here. */
    private static <T> T newLookupEntity(Class<T> type, UUID id, String code, String label) throws Exception {
        T entity = type.getDeclaredConstructor().newInstance();
        setField(entity, "id", id);
        setField(entity, "code", code);
        setField(entity, "label", label);
        return entity;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
