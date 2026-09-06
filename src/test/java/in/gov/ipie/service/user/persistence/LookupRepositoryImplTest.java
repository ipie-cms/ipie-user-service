package in.gov.ipie.service.user.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.service.user.domain.LookupCatalogue;
import in.gov.ipie.service.user.domain.LookupOption;

/**
 * The entity-to-{@link LookupOption} mapping that used to live in
 * {@code RegistrationLookupServiceImpl}, tested where it now lives.
 */
class LookupRepositoryImplTest {

    private final ProfessionalRoleJpaRepository professionalRoleRepository = mock(ProfessionalRoleJpaRepository.class);
    private final LegalRepresentativeTypeJpaRepository legalRepresentativeTypeRepository =
            mock(LegalRepresentativeTypeJpaRepository.class);
    private final ProfessionalIdentificationTypeJpaRepository professionalIdentificationTypeRepository =
            mock(ProfessionalIdentificationTypeJpaRepository.class);
    private final IdentityProofTypeJpaRepository identityProofTypeRepository = mock(IdentityProofTypeJpaRepository.class);

    private final LookupRepositoryImpl repository = new LookupRepositoryImpl(
            professionalRoleRepository, legalRepresentativeTypeRepository, professionalIdentificationTypeRepository,
            identityProofTypeRepository);

    @Test
    void findActiveOptions_mapsProfessionalRoleRowsInSortOrder() throws Exception {
        UUID id = UUID.randomUUID();
        ProfessionalRoleJpaEntity entity = newLookupEntity(ProfessionalRoleJpaEntity.class, id, "INSOLVENCY_PROFESSIONAL",
                "Insolvency Professional");
        when(professionalRoleRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(entity));

        assertThat(repository.findActiveOptions(LookupCatalogue.PROFESSIONAL_ROLE))
                .containsExactly(new LookupOption(id, "INSOLVENCY_PROFESSIONAL", "Insolvency Professional"));
    }

    @Test
    void findActiveOptions_mapsIdentityProofRows() throws Exception {
        UUID id = UUID.randomUUID();
        IdentityProofTypeJpaEntity entity = newLookupEntity(IdentityProofTypeJpaEntity.class, id, "PAN", "PAN Card");
        when(identityProofTypeRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(entity));

        assertThat(repository.findActiveOptions(LookupCatalogue.IDENTITY_PROOF_TYPE))
                .containsExactly(new LookupOption(id, "PAN", "PAN Card"));
    }

    @Test
    void findActiveOptions_returnsEmptyWhenCatalogueHasNoActiveRows() {
        when(legalRepresentativeTypeRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of());

        assertThat(repository.findActiveOptions(LookupCatalogue.LEGAL_REPRESENTATIVE_TYPE)).isEmpty();
    }

    /**
     * Every catalogue constant resolves. The switch in the adapter has no {@code default}, so a
     * new constant is a compile error rather than a runtime surprise - this guards the mapping
     * that remains: that each constant reaches the repository it should.
     */
    @Test
    void everyCatalogueResolves() {
        for (LookupCatalogue catalogue : LookupCatalogue.values()) {
            assertThatCode(() -> repository.findActiveOptions(catalogue))
                    .describedAs("catalogue %s does not resolve", catalogue)
                    .doesNotThrowAnyException();
        }
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
