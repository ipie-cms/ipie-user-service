package in.gov.ipie.service.user.service;

import java.util.List;

import in.gov.ipie.service.user.domain.LookupOption;

/**
 * Reads the registration wizard's database-backed dropdown catalogues (see {@code
 * LookupJpaEntity}'s Javadoc) - each list is active rows only, sorted for display. See {@link
 * RegistrationLookupServiceImpl} for the implementation.
 */
public interface RegistrationLookupService {

    List<LookupOption> listProfessionalRoles();

    List<LookupOption> listLegalRepresentativeTypes();

    List<LookupOption> listProfessionalIdentificationTypes();

    List<LookupOption> listIdentityProofTypes();
}
