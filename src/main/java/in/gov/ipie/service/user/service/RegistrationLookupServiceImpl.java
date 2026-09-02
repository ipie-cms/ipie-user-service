package in.gov.ipie.service.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.service.user.domain.LookupOption;
import in.gov.ipie.service.user.persistence.IdentityProofTypeJpaRepository;
import in.gov.ipie.service.user.persistence.LegalRepresentativeTypeJpaRepository;
import in.gov.ipie.service.user.persistence.LookupJpaEntity;
import in.gov.ipie.service.user.persistence.ProfessionalIdentificationTypeJpaRepository;
import in.gov.ipie.service.user.persistence.ProfessionalRoleJpaRepository;

/**
 * {@link RegistrationLookupService} implementation. Reads straight off the 4 lookup JPA
 * repositories - deliberately skips the domain-model/repository-interface split {@code
 * UserServiceImpl}/{@code OrganisationServiceImpl} use, since these tables carry no business
 * behavior beyond "list the active rows" (see {@code LookupJpaEntity}'s Javadoc).
 */
@Service
public class RegistrationLookupServiceImpl implements RegistrationLookupService {

    private final ProfessionalRoleJpaRepository professionalRoleRepository;
    private final LegalRepresentativeTypeJpaRepository legalRepresentativeTypeRepository;
    private final ProfessionalIdentificationTypeJpaRepository professionalIdentificationTypeRepository;
    private final IdentityProofTypeJpaRepository identityProofTypeRepository;

    public RegistrationLookupServiceImpl(
            ProfessionalRoleJpaRepository professionalRoleRepository,
            LegalRepresentativeTypeJpaRepository legalRepresentativeTypeRepository,
            ProfessionalIdentificationTypeJpaRepository professionalIdentificationTypeRepository,
            IdentityProofTypeJpaRepository identityProofTypeRepository) {
        this.professionalRoleRepository = professionalRoleRepository;
        this.legalRepresentativeTypeRepository = legalRepresentativeTypeRepository;
        this.professionalIdentificationTypeRepository = professionalIdentificationTypeRepository;
        this.identityProofTypeRepository = identityProofTypeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> listProfessionalRoles() {
        return toOptions(professionalRoleRepository.findByActiveTrueOrderBySortOrderAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> listLegalRepresentativeTypes() {
        return toOptions(legalRepresentativeTypeRepository.findByActiveTrueOrderBySortOrderAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> listProfessionalIdentificationTypes() {
        return toOptions(professionalIdentificationTypeRepository.findByActiveTrueOrderBySortOrderAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> listIdentityProofTypes() {
        return toOptions(identityProofTypeRepository.findByActiveTrueOrderBySortOrderAsc());
    }

    private static List<LookupOption> toOptions(List<? extends LookupJpaEntity> entities) {
        return entities.stream().map(entity -> new LookupOption(entity.getId(), entity.getCode(), entity.getLabel())).toList();
    }
}
