package in.gov.ipie.service.user.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.common.web.paging.PageResponse;
import in.gov.ipie.service.user.mapper.OrganisationApiMapper;
import in.gov.ipie.service.user.mapper.UserApiMapper;
import in.gov.ipie.service.user.dto.request.CompleteRegistrationRequest;
import in.gov.ipie.service.user.dto.request.ConfirmEmailOtpRequest;
import in.gov.ipie.service.user.dto.request.CreateRegistrationRequest;
import in.gov.ipie.service.user.dto.request.SaveRegistrationDraftRequest;
import in.gov.ipie.service.user.dto.response.EmailOtpStatusResponse;
import in.gov.ipie.service.user.dto.response.OrganisationResponse;
import in.gov.ipie.service.user.dto.response.RegistrationResponse;
import in.gov.ipie.service.user.dto.response.UserResponse;
import in.gov.ipie.service.user.domain.Organisation;
import in.gov.ipie.service.user.service.OrganisationService;
import in.gov.ipie.service.user.service.UserService;
import in.gov.ipie.service.user.domain.User;

/**
 * The self-registration flow - deliberately unauthenticated (see {@code
 * ipie.security.public-paths} in {@code application.yml}): this is how an unauthenticated visitor
 * gets an account in the first place, and the verification link is a plain URL a pillar
 * admin clicks from their email client, not an API call carrying a bearer token.
 */
@RestController
public class RegistrationController {

    private final UserService userService;
    private final UserApiMapper userApiMapper;
    private final OrganisationService organisationService;
    private final OrganisationApiMapper organisationApiMapper;

    public RegistrationController(
            UserService userService, UserApiMapper userApiMapper, OrganisationService organisationService,
            OrganisationApiMapper organisationApiMapper) {
        this.userService = userService;
        this.userApiMapper = userApiMapper;
        this.organisationService = organisationService;
        this.organisationApiMapper = organisationApiMapper;
    }

    /**
     * The Entity registration wizard's "Search and Select Your Registered Entity" step - a
     * read-only lookup, deliberately exposed here (not through {@code OrganisationController}'s
     * own {@code ORGANISATION_READ}-gated endpoint) since the wizard runs fully unauthenticated.
     */
    @GetMapping("/api/v1/registrations/organisations/search")
    public PageResponse<OrganisationResponse> searchOrganisations(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<Organisation> result = organisationService.searchOrganisations(name, PageRequest.of(page, size));
        return PageResponse.from(result, organisationApiMapper::toResponse);
    }

    /** Step 1: mobile number + email only. */
    @PostMapping("/api/v1/registrations")
    public ResponseEntity<RegistrationResponse> createRegistration(@Valid @RequestBody CreateRegistrationRequest request) {
        User created = userService.createRegistration(userApiMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(userApiMapper.toRegistrationResponse(created));
    }

    /** The registration wizard's "Save Draft" action - callable repeatedly from any step. */
    @PatchMapping("/api/v1/registrations/{id}")
    public UserResponse saveRegistrationDraft(
            @PathVariable("id") UUID registrationId, @Valid @RequestBody SaveRegistrationDraftRequest request) {
        User saved = userService.saveRegistrationDraft(userApiMapper.toCommand(registrationId, request));
        return userApiMapper.toResponse(saved);
    }

    /** The wizard's Email "SEND OTP" - emails a fresh code to the registrant's own address. */
    @PostMapping("/api/v1/registrations/{id}/email-otp")
    public ResponseEntity<Void> requestEmailOtp(@PathVariable("id") UUID registrationId) {
        userService.requestEmailOtp(registrationId);
        return ResponseEntity.accepted().build();
    }

    /** The registrant submitted the emailed code. */
    @PostMapping("/api/v1/registrations/{id}/email-otp/confirm")
    public EmailOtpStatusResponse confirmEmailOtp(
            @PathVariable("id") UUID registrationId, @Valid @RequestBody ConfirmEmailOtpRequest request) {
        userService.confirmEmailOtp(userApiMapper.toCommand(registrationId, request));
        return new EmailOtpStatusResponse(true);
    }

    /** Step 2 ("SUBMIT FOR VERIFICATION") - provisions the Keycloak account and moves to UNVERIFIED. */
    @PostMapping("/api/v1/registrations/{id}/complete")
    public UserResponse completeRegistration(
            @PathVariable("id") UUID registrationId, @Valid @RequestBody CompleteRegistrationRequest request) {
        User completed = userService.completeRegistration(userApiMapper.toCommand(registrationId, request));
        return userApiMapper.toResponse(completed);
    }

    /** Followed from the pillar-admin verification email. */
    @GetMapping("/api/v1/users/verify")
    public UserResponse verify(@RequestParam String token) {
        return userApiMapper.toResponse(userService.verifyByToken(token));
    }
}
