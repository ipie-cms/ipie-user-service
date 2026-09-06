package in.gov.ipie.service.user.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import in.gov.ipie.service.user.domain.LookupCatalogue;
import in.gov.ipie.service.user.domain.LookupOption;
import in.gov.ipie.service.user.repository.LookupRepository;

/**
 * JPA adapter for {@link LookupRepository}. Holds the four Spring Data repositories the
 * application layer used to inject directly, and is the only place {@link LookupJpaEntity} is
 * mapped to {@link LookupOption}.
 *
 * <p>The catalogue-to-table dispatch is a {@code switch} over {@link LookupCatalogue} with no
 * {@code default}, so adding a constant to that enum stops this class compiling until its table is
 * wired here. That is the whole reason it is a switch and not a map: a map lookup can only fail at
 * runtime, as an empty dropdown in the registration wizard that reads like missing seed data.
 *
 * <p>It is also why this class is neither {@code final} nor built in its constructor. The first
 * version populated an {@code EnumMap} there, which SpotBugs flagged as
 * {@code CT_CONSTRUCTOR_THROW} - a throwing constructor on a subclassable type - and the remedy
 * applied was {@code final}. That silenced SpotBugs and broke the application at startup: Spring
 * proxies this bean and CGLIB cannot subclass a final class, so every integration test failed with
 * "Cannot subclass final class". Nothing but a full context load catches that, which is to say
 * nothing catches it without Docker. Assigning fields and switching at the call site removes the
 * throwing constructor instead of sealing the class against it, so there is nothing left to
 * suppress and nothing left to break.
 */
@Repository
public class LookupRepositoryImpl implements LookupRepository {

    private final ProfessionalRoleJpaRepository professionalRoleRepository;
    private final LegalRepresentativeTypeJpaRepository legalRepresentativeTypeRepository;
    private final ProfessionalIdentificationTypeJpaRepository professionalIdentificationTypeRepository;
    private final IdentityProofTypeJpaRepository identityProofTypeRepository;

    public LookupRepositoryImpl(
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
    public List<LookupOption> findActiveOptions(LookupCatalogue catalogue) {
        List<? extends LookupJpaEntity> rows = switch (catalogue) {
            case PROFESSIONAL_ROLE -> professionalRoleRepository.findByActiveTrueOrderBySortOrderAsc();
            case LEGAL_REPRESENTATIVE_TYPE -> legalRepresentativeTypeRepository.findByActiveTrueOrderBySortOrderAsc();
            case PROFESSIONAL_IDENTIFICATION_TYPE ->
                    professionalIdentificationTypeRepository.findByActiveTrueOrderBySortOrderAsc();
            case IDENTITY_PROOF_TYPE -> identityProofTypeRepository.findByActiveTrueOrderBySortOrderAsc();
        };
        return rows.stream()
                .map(entity -> new LookupOption(entity.getId(), entity.getCode(), entity.getLabel()))
                .toList();
    }
}
