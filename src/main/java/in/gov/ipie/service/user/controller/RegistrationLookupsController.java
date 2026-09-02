package in.gov.ipie.service.user.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.service.user.domain.LookupOption;
import in.gov.ipie.service.user.dto.response.LookupOptionResponse;
import in.gov.ipie.service.user.service.RegistrationLookupService;

/**
 * The registration wizard's database-backed dropdown catalogues - deliberately public (under
 * {@code /api/v1/registrations/**}, already unauthenticated - see {@code
 * ipie.security.public-paths}), same reasoning as {@link RegistrationController}: the wizard runs
 * before the visitor has any account. Adding a new option to any of these catalogues is a seed-data
 * insert into the matching table, not a code change - see {@code LookupJpaEntity}'s Javadoc.
 */
@RestController
@RequestMapping("/api/v1/registrations")
public class RegistrationLookupsController {

    private final RegistrationLookupService lookupService;

    public RegistrationLookupsController(RegistrationLookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/professional-roles")
    public List<LookupOptionResponse> professionalRoles() {
        return toResponses(lookupService.listProfessionalRoles());
    }

    @GetMapping("/legal-representative-types")
    public List<LookupOptionResponse> legalRepresentativeTypes() {
        return toResponses(lookupService.listLegalRepresentativeTypes());
    }

    @GetMapping("/professional-identification-types")
    public List<LookupOptionResponse> professionalIdentificationTypes() {
        return toResponses(lookupService.listProfessionalIdentificationTypes());
    }

    @GetMapping("/identity-proof-types")
    public List<LookupOptionResponse> identityProofTypes() {
        return toResponses(lookupService.listIdentityProofTypes());
    }

    private static List<LookupOptionResponse> toResponses(List<LookupOption> options) {
        return options.stream()
                .map(option -> new LookupOptionResponse(option.id().toString(), option.code(), option.label()))
                .toList();
    }
}
