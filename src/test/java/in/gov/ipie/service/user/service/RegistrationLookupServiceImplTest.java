package in.gov.ipie.service.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.service.user.domain.LookupCatalogue;
import in.gov.ipie.service.user.domain.LookupOption;
import in.gov.ipie.service.user.repository.LookupRepository;

/**
 * What is left to test here after the port extraction (2026-09-05): that each method asks for the
 * right catalogue and returns what the port gave it. Mapping a row to a {@link LookupOption} moved
 * to the adapter and is covered by {@code LookupRepositoryImplTest}.
 *
 * <p>That split is the point of the refactor showing up in the tests: this test no longer needs
 * four Spring Data mocks and reflection to build JPA entities in order to assert a
 * catalogue-selection decision.
 */
class RegistrationLookupServiceImplTest {

    private final LookupRepository lookupRepository = mock(LookupRepository.class);

    private final RegistrationLookupServiceImpl lookupService = new RegistrationLookupServiceImpl(lookupRepository);

    @Test
    void listProfessionalRoles_readsTheProfessionalRoleCatalogue() {
        LookupOption option = new LookupOption(UUID.randomUUID(), "INSOLVENCY_PROFESSIONAL", "Insolvency Professional");
        when(lookupRepository.findActiveOptions(LookupCatalogue.PROFESSIONAL_ROLE)).thenReturn(List.of(option));

        assertThat(lookupService.listProfessionalRoles()).containsExactly(option);
    }

    @Test
    void listIdentityProofTypes_readsTheIdentityProofCatalogue() {
        LookupOption option = new LookupOption(UUID.randomUUID(), "PAN", "PAN Card");
        when(lookupRepository.findActiveOptions(LookupCatalogue.IDENTITY_PROOF_TYPE)).thenReturn(List.of(option));

        assertThat(lookupService.listIdentityProofTypes()).containsExactly(option);
    }

    @Test
    void listLegalRepresentativeTypes_readsTheLegalRepresentativeCatalogue() {
        LookupOption option = new LookupOption(UUID.randomUUID(), "DIRECTOR", "Director");
        when(lookupRepository.findActiveOptions(LookupCatalogue.LEGAL_REPRESENTATIVE_TYPE)).thenReturn(List.of(option));

        assertThat(lookupService.listLegalRepresentativeTypes()).containsExactly(option);
    }

    @Test
    void listProfessionalIdentificationTypes_readsTheProfessionalIdentificationCatalogue() {
        LookupOption option = new LookupOption(UUID.randomUUID(), "IBBI_REG_NO", "IBBI registration number");
        when(lookupRepository.findActiveOptions(LookupCatalogue.PROFESSIONAL_IDENTIFICATION_TYPE))
                .thenReturn(List.of(option));

        assertThat(lookupService.listProfessionalIdentificationTypes()).containsExactly(option);
    }
}
