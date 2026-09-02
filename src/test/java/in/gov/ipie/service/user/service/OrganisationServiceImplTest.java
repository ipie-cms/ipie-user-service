package in.gov.ipie.service.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.service.user.command.CreateOrganisationCommand;
import in.gov.ipie.service.user.domain.LegalConstitution;
import in.gov.ipie.service.user.domain.Organisation;
import in.gov.ipie.service.user.domain.OrganisationIdType;
import in.gov.ipie.service.user.exception.OrganisationNotFoundException;
import in.gov.ipie.service.user.repository.OrganisationRepository;

/**
 * {@link OrganisationServiceImpl#findOrCreate}'s dedup logic is the one genuinely new business
 * rule this class adds - "system should ensure that no duplication of entity records happen"
 * (FRS 1.1.1) - so this test focuses there rather than repeating CRUD coverage already proven
 * elsewhere in this codebase's other services.
 */
class OrganisationServiceImplTest {

    private final OrganisationRepository organisationRepository = mock(OrganisationRepository.class);
    private final OrganisationServiceImpl organisationService = new OrganisationServiceImpl(organisationRepository);

    private static final CreateOrganisationCommand COMMAND = new CreateOrganisationCommand(
            "Acme Insolvency Advisors LLP", LegalConstitution.LLP, OrganisationIdType.LLPIN, "AAA-1234", false, null,
            "123 Example Street", "+91 9800000000", "contact@example.com", "India", "Maharashtra", "Mumbai", "400001", null);

    @Test
    void findOrCreate_returnsTheExistingOrganisation_whenOneAlreadyExistsForThatIdTypeAndValue() {
        Organisation existing = Organisation.builder()
                .id(UUID.randomUUID())
                .name("Acme Insolvency Advisors LLP")
                .legalConstitution(LegalConstitution.LLP)
                .idType(OrganisationIdType.LLPIN)
                .idValue("AAA-1234")
                .auditMetadata(new AuditMetadata(Instant.now(), "system", Instant.now(), "system", 0, true, null, null))
                .build();
        when(organisationRepository.findByIdTypeAndIdValue(OrganisationIdType.LLPIN, "AAA-1234")).thenReturn(Optional.of(existing));

        Organisation result = organisationService.findOrCreate(COMMAND);

        assertThat(result.getId()).isEqualTo(existing.getId());
        verify(organisationRepository, never()).save(any());
    }

    @Test
    void findOrCreate_createsANewOrganisation_whenNoneExistsForThatIdTypeAndValue() {
        when(organisationRepository.findByIdTypeAndIdValue(OrganisationIdType.LLPIN, "AAA-1234")).thenReturn(Optional.empty());
        when(organisationRepository.save(any())).thenAnswer(invocation -> {
            Organisation organisation = invocation.getArgument(0);
            AuditMetadata auditMetadata = new AuditMetadata(Instant.now(), "system", Instant.now(), "system", 0, true, null, null);
            return organisation.toBuilder().id(UUID.randomUUID()).auditMetadata(auditMetadata).build();
        });

        Organisation result = organisationService.findOrCreate(COMMAND);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Acme Insolvency Advisors LLP");
        assertThat(result.getCountry()).isEqualTo("India");
        assertThat(result.getState()).isEqualTo("Maharashtra");
        verify(organisationRepository, times(1)).save(any());
    }

    @Test
    void getOrganisation_throwsNotFound_whenNoSuchOrganisationExists() {
        UUID missingId = UUID.randomUUID();
        when(organisationRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organisationService.getOrganisation(missingId)).isInstanceOf(OrganisationNotFoundException.class);
    }
}
