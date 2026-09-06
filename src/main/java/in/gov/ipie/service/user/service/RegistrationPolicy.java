package in.gov.ipie.service.user.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.exception.FieldError;
import in.gov.ipie.common.core.exception.ValidationFailedException;
import in.gov.ipie.service.user.domain.LookupOption;
import in.gov.ipie.service.user.domain.ProfessionalRoleHolding;

/**
 * The rules a registration is held to: how long its secrets live, how many attempts it gets, and
 * which combinations of professional roles are acceptable.
 *
 * <p>Extracted from {@code UserServiceImpl}, which took four separate {@code @Value} parameters and
 * a lookup service used for nothing else - five constructor arguments for one subject. The limit on
 * constructor parameters is what surfaced it, but the grouping stands on its own: these are the
 * knobs and rules of registration, and a reader looking for "how long is an OTP valid" now has one
 * place to look rather than a constructor signature to scan.
 */
@Component
public class RegistrationPolicy {

    /** The one role a legal-representative type may qualify - matched by code, since ids are per environment. */
    private static final String LEGAL_REPRESENTATIVE_CODE = "LEGAL_REPRESENTATIVE";

    private final RegistrationLookupService registrationLookupService;
    private final Duration verificationTokenTtl;
    private final Duration emailOtpTtl;
    private final int emailOtpMaxAttempts;
    private final int emailOtpMaxResends;

    public RegistrationPolicy(
            RegistrationLookupService registrationLookupService,
            @Value("${ipie.registration.verification-token-ttl:PT48H}") Duration verificationTokenTtl,
            @Value("${ipie.registration.email-otp-ttl:PT10M}") Duration emailOtpTtl,
            @Value("${ipie.registration.email-otp-max-attempts:5}") int emailOtpMaxAttempts,
            @Value("${ipie.registration.email-otp-max-resends:5}") int emailOtpMaxResends) {
        this.registrationLookupService = registrationLookupService;
        this.verificationTokenTtl = verificationTokenTtl;
        this.emailOtpTtl = emailOtpTtl;
        this.emailOtpMaxAttempts = emailOtpMaxAttempts;
        this.emailOtpMaxResends = emailOtpMaxResends;
    }

    /** How long the emailed set-password verification token stays usable. */
    public Duration verificationTokenTtl() {
        return verificationTokenTtl;
    }

    /** How long one email OTP stays usable. */
    public Duration emailOtpTtl() {
        return emailOtpTtl;
    }

    /** Guesses allowed against one code before it is discarded. */
    public int emailOtpMaxAttempts() {
        return emailOtpMaxAttempts;
    }

    /** New codes a registration may ask for. */
    public int emailOtpMaxResends() {
        return emailOtpMaxResends;
    }

    /**
     * Rules about the professional roles as a <em>set</em>, which bean validation on the request
     * cannot express because each rule needs more than one entry, or needs the catalogue.
     *
     * <p>Two of them. The same role claimed twice is meaningless and the database refuses it anyway
     * (unique on user and role) - caught here so the caller gets a field error naming the problem
     * rather than a constraint violation naming an index. And a legal-representative type - Advocate,
     * CA, CS - qualifies the LEGAL_REPRESENTATIVE role alone; attached to any other role it is a
     * claim about a role the person did not claim, so it is refused rather than quietly dropped.
     */
    public void validateProfessionalRoles(List<ProfessionalRoleHolding> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        List<FieldError> errors = new ArrayList<>();

        Set<UUID> seen = new HashSet<>();
        for (ProfessionalRoleHolding role : roles) {
            if (role.roleId() != null && !seen.add(role.roleId())) {
                errors.add(new FieldError("professionalRoles",
                        "The same professional role is claimed more than once"));
                break;
            }
        }

        UUID legalRepresentativeRoleId = registrationLookupService.listProfessionalRoles().stream()
                .filter(option -> LEGAL_REPRESENTATIVE_CODE.equals(option.code()))
                .map(LookupOption::id)
                .findFirst()
                .orElse(null);
        for (ProfessionalRoleHolding role : roles) {
            if (role.legalRepresentativeTypeId() != null
                    && !Objects.equals(role.roleId(), legalRepresentativeRoleId)) {
                errors.add(new FieldError("professionalRoles",
                        "A legal representative type may only be set on the LEGAL_REPRESENTATIVE role"));
                break;
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationFailedException("The professional roles submitted are not valid", errors);
        }
    }
}
