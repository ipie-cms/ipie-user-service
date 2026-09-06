package in.gov.ipie.service.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.service.user.domain.LookupCatalogue;
import in.gov.ipie.service.user.domain.LookupOption;
import in.gov.ipie.service.user.repository.LookupRepository;

/**
 * {@link RegistrationLookupService} implementation. Each method names one catalogue and hands it
 * to {@link LookupRepository}; which table backs a catalogue, and how a row becomes a
 * {@link LookupOption}, belong to the adapter.
 *
 * <p>This class used to inject the four Spring Data repositories and map {@code LookupJpaEntity}
 * here, deliberately skipping the port/adapter split on the grounds that these tables carry no
 * business behavior. See {@link LookupRepository}'s Javadoc for why that was undone on
 * 2026-09-05 - briefly, the shortcut was defensible but it put a persistence type in the
 * application layer, and it survived only because no ArchUnit rule looked at this package.
 */
@Service
public class RegistrationLookupServiceImpl implements RegistrationLookupService {

    private final LookupRepository lookupRepository;

    public RegistrationLookupServiceImpl(LookupRepository lookupRepository) {
        this.lookupRepository = lookupRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> listProfessionalRoles() {
        return lookupRepository.findActiveOptions(LookupCatalogue.PROFESSIONAL_ROLE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> listLegalRepresentativeTypes() {
        return lookupRepository.findActiveOptions(LookupCatalogue.LEGAL_REPRESENTATIVE_TYPE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> listProfessionalIdentificationTypes() {
        return lookupRepository.findActiveOptions(LookupCatalogue.PROFESSIONAL_IDENTIFICATION_TYPE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> listIdentityProofTypes() {
        return lookupRepository.findActiveOptions(LookupCatalogue.IDENTITY_PROOF_TYPE);
    }
}
