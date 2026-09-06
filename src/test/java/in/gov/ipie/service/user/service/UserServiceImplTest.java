package in.gov.ipie.service.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.events.outbox.OutboxStore;
import in.gov.ipie.service.user.command.CompleteRegistrationCommand;
import in.gov.ipie.service.user.command.ConfirmEmailOtpCommand;
import in.gov.ipie.service.user.command.CreateOrganisationCommand;
import in.gov.ipie.service.user.command.CreateRegistrationCommand;
import in.gov.ipie.service.user.command.CreateUserCommand;
import in.gov.ipie.service.user.command.EntityDraftDetails;
import in.gov.ipie.service.user.command.SaveRegistrationDraftCommand;
import in.gov.ipie.service.user.domain.ProfessionalRoleHolding;
import in.gov.ipie.service.user.domain.LegalConstitution;
import in.gov.ipie.service.user.domain.Organisation;
import in.gov.ipie.service.user.domain.OrganisationIdType;
import in.gov.ipie.service.user.exception.EmailNotVerifiedException;
import in.gov.ipie.service.user.exception.InvalidOtpException;
import in.gov.ipie.service.user.consent.ConsentRecorder;
import in.gov.ipie.service.user.exception.EmailOtpAttemptsExhaustedException;
import in.gov.ipie.service.user.exception.EmailOtpResendLimitReachedException;
import in.gov.ipie.service.user.exception.InvalidVerificationTokenException;
import in.gov.ipie.service.user.security.RegistrationSecretHasher;
import in.gov.ipie.service.user.exception.RegistrationAlreadyCompletedException;
import in.gov.ipie.service.user.exception.UserNotFoundException;
import in.gov.ipie.service.user.domain.RegistrationStatus;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.UserStatus;
import in.gov.ipie.service.user.repository.UserRepository;
import in.gov.ipie.service.user.repository.UserSearchIndex;
import in.gov.ipie.service.user.domain.VisibilityScope;

/** Username/email/mobile uniqueness is validated by {@code UserValidationAspect} - see {@code UserValidationAspectTest}, not here. */
class UserServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserSearchIndex userSearchIndex = mock(UserSearchIndex.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private final OrganisationService organisationService = mock(OrganisationService.class);
    /** Same pepper as the instance under test - static so the static fixtures below can use it too. */
    private static final RegistrationSecretHasher TEST_HASHER = new RegistrationSecretHasher("test-pepper");

    private final ConsentRecorder consentRecorder = mock(ConsentRecorder.class);
    private final RegistrationLookupService registrationLookupService = mock(RegistrationLookupService.class);

    private RegistrationSecretHasher secretHasher;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // A real hasher, deliberately, not a mock: what these tests care about is whether a
        // presented code still matches what was stored once only the digest is kept.
        secretHasher = TEST_HASHER;
        when(registrationLookupService.listProfessionalRoles()).thenReturn(List.of());
        UserEventPublisher eventPublisher = new UserEventPublisher(outboxStore, "ipie-user-service-test");
        RegistrationPolicy registrationPolicy = new RegistrationPolicy(
                registrationLookupService, Duration.ofHours(48), Duration.ofMinutes(10), 5, 5);
        userService = new UserServiceImpl(
                userRepository, userSearchIndex, eventPublisher, organisationService, secretHasher,
                consentRecorder, registrationPolicy);
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
            AuditMetadata auditMetadata = new AuditMetadata(Instant.now(), "system", Instant.now(), "system", 0, true, null, null);
            return user.toBuilder().id(id).auditMetadata(auditMetadata).build();
        });
    }

    /** Minimal {@link CompleteRegistrationCommand} - only fullName/password set, every rich field {@code null}. */
    private static CompleteRegistrationCommand completeCommand(UUID registrationId, String fullName) {
        return new CompleteRegistrationCommand(
                registrationId, // registrationId
                fullName, // fullName
                null, // category
                null, // addressLine1
                null, // addressLine2
                null, // country
                null, // state
                null, // city
                null, // pin
                null, // identityProofTypeId
                null, // identityProofNumber
                List.of(), // professionalRoles
                null, // organisationId
                null); // entity
    }

    @Test
    void createUser_savesAndPublishesEvent() {
        User created = userService.createUser(new CreateUserCommand("jdoe", "jdoe@example.com", "Jane Doe", null, null));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userSearchIndex).index(created);
        verify(outboxStore).save(any());
    }

    @Test
    void getUser_throwsNotFound_whenMissing() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(missingId)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void searchUsersAfter_delegatesToUserSearchIndex() {
        CursorPageRequest pageRequest = CursorPageRequest.firstPage(20);
        CursorPageResult<User> expected = CursorPageResult.of(List.of(), null, false);
        when(userSearchIndex.searchAfter(eq(UserSearchCriteria.empty()), any(), eq(pageRequest))).thenReturn(expected);

        CursorPageResult<User> result = userService.searchUsersAfter(
                UserSearchCriteria.empty(), VisibilityScope.unrestricted(null), pageRequest);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void deactivateUser_flipsStatusToInactive() {
        UUID userId = UUID.randomUUID();
        AuditMetadata auditMetadata = new AuditMetadata(Instant.now(), "system", Instant.now(), "system", 0, true, null, null);
        User activeUser = User.builder()
                .id(userId).username("jdoe").email("jdoe@example.com").fullName("Jane Doe")
                .status(UserStatus.ACTIVE).registrationStatus(RegistrationStatus.VERIFIED).auditMetadata(auditMetadata)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        User deactivated = userService.deactivateUser(userId, "no longer needed");

        assertThat(deactivated.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(outboxStore).save(any());
    }

    @Test
    void createRegistration_savesPreRegistrationUser() {
        User created = userService.createRegistration(new CreateRegistrationCommand("+91 9800000009", "newcomer@example.com", null));

        assertThat(created.getRegistrationStatus()).isEqualTo(RegistrationStatus.PRE_REGISTRATION);
        assertThat(created.getUsername()).isEqualTo("newcomer@example.com");
        verify(outboxStore, never()).save(any());
    }

    /** Every {@code completeRegistration} test needs the email OTP already confirmed - see {@code EmailNotVerifiedException}. */
    private static User emailVerifiedPreRegistration(UUID registrationId) {
        User preRegistered = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(registrationId).build();
        preRegistered.requestEmailOtp(TEST_HASHER.hash("123456"), Instant.now().plusSeconds(600));
        preRegistered.confirmEmailOtp();
        return preRegistered;
    }

    @Test
    void completeRegistration_requestsProvisioningAsynchronouslyAndDoesNotWaitForTheAccount() {
        UUID registrationId = UUID.randomUUID();
        User preRegistered = emailVerifiedPreRegistration(registrationId);
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));

        User completed = userService.completeRegistration(completeCommand(registrationId, "Jane Doe"));

        // PROVISIONING, not UNVERIFIED: the Keycloak account does not exist yet. This method now
        // completes on this service's own database write and publishes a request - it makes no
        // cross-service call, so registration no longer fails when Keycloak is busy.
        assertThat(completed.getRegistrationStatus()).isEqualTo(RegistrationStatus.PROVISIONING);
        assertThat(completed.getKeycloakUserId()).isNull();
        // The pillar admin's approval token, and only that - this service holds no
        // credential-setting token (ARCHITECTURE_WORKING_PLAN.md, 4.1.1).
        assertThat(completed.getVerificationTokenHash()).isNull();  // minted at accountProvisioned, not here
        verify(outboxStore).save(any());
    }

    @Test
    void completeRegistration_throwsConflict_whenNotInPreRegistrationStatus() {
        UUID registrationId = UUID.randomUUID();
        User alreadyVerified = User.createNew("jdoe", "jdoe@example.com", "Jane Doe", null).toBuilder().id(registrationId).build();
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(alreadyVerified));

        assertThatThrownBy(() -> userService.completeRegistration(completeCommand(registrationId, "Jane Doe")))
                .isInstanceOf(RegistrationAlreadyCompletedException.class);
    }

    @Test
    void completeRegistration_throwsEmailNotVerified_whenOtpNotConfirmed() {
        UUID registrationId = UUID.randomUUID();
        User preRegistered = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(registrationId).build();
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));

        assertThatThrownBy(() -> userService.completeRegistration(completeCommand(registrationId, "Jane Doe")))
                .isInstanceOf(EmailNotVerifiedException.class);
        verify(outboxStore, never()).save(any());
    }

    @Test
    void completeRegistration_findsOrCreatesOrganisation_whenEntityDetailsPresent() {
        UUID registrationId = UUID.randomUUID();
        UUID organisationId = UUID.randomUUID();
        User preRegistered = emailVerifiedPreRegistration(registrationId);
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));
        AuditMetadata orgAudit = new AuditMetadata(Instant.now(), "system", Instant.now(), "system", 0, true, null, null);
        Organisation organisation = Organisation.builder()
                .id(organisationId).name("ABC Company").legalConstitution(LegalConstitution.PRIVATE_LTD_COMPANY)
                .idType(OrganisationIdType.CIN).idValue("U12345MH2020PTC000001").auditMetadata(orgAudit).build();
        when(organisationService.findOrCreate(any())).thenReturn(organisation);
        EntityDraftDetails entity = new EntityDraftDetails(
                "ABC Company", LegalConstitution.PRIVATE_LTD_COMPANY, OrganisationIdType.CIN, "U12345MH2020PTC000001",
                false, null, "1 Main St", null, null, "India", "Maharashtra", "Mumbai", "400001", null);
        UUID professionalRoleId = UUID.randomUUID();
        CompleteRegistrationCommand command = new CompleteRegistrationCommand(
                registrationId, // registrationId
                "Jane Doe", // fullName
                null, // category
                null, // addressLine1
                null, // addressLine2
                null, // country
                null, // state
                null, // city
                null, // pin
                null, // identityProofTypeId
                null, // identityProofNumber
                List.of(ProfessionalRoleHolding.builder().roleId(professionalRoleId).build()), // professionalRoles
                null, // organisationId
                entity); // entity

        User completed = userService.completeRegistration(command);

        assertThat(completed.getOrganisationId()).isEqualTo(organisationId);
        assertThat(completed.getProfessionalRoles())
                .extracting(ProfessionalRoleHolding::roleId)
                .containsExactly(professionalRoleId);
        verify(organisationService).findOrCreate(any(CreateOrganisationCommand.class));
    }

    @Test
    void saveRegistrationDraft_overwritesDraftFieldsWithoutCompletingRegistration() {
        UUID registrationId = UUID.randomUUID();
        User preRegistered = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(registrationId).build();
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));
        SaveRegistrationDraftCommand command = new SaveRegistrationDraftCommand(
                registrationId, // registrationId
                "Jane Doe", // fullName
                null, // category
                "1 Main St", // addressLine1
                null, // addressLine2
                "India", // country
                "Maharashtra", // state
                "Mumbai", // city
                "400001", // pin
                null, // identityProofTypeId
                null, // identityProofNumber
                List.of(), // professionalRoles
                null, // organisationId
                null); // entity

        User saved = userService.saveRegistrationDraft(command);

        assertThat(saved.getFullName()).isEqualTo("Jane Doe");
        assertThat(saved.getAddressLine1()).isEqualTo("1 Main St");
        assertThat(saved.getRegistrationStatus()).isEqualTo(RegistrationStatus.PRE_REGISTRATION);
        verify(outboxStore, never()).save(any());
    }

    @Test
    void requestEmailOtp_storesCodeAndPublishesEvent() {
        UUID registrationId = UUID.randomUUID();
        User preRegistered = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(registrationId).build();
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));

        User updated = userService.requestEmailOtp(registrationId);

        assertThat(updated.getEmailOtpCodeHash()).matches("[0-9a-f]{64}");
        assertThat(updated.getEmailOtpExpiresAt()).isAfter(Instant.now());
        verify(outboxStore).save(any());
    }

    @Test
    void confirmEmailOtp_marksEmailVerified_whenCodeMatchesAndNotExpired() {
        UUID registrationId = UUID.randomUUID();
        User preRegistered = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(registrationId).build();
        preRegistered.requestEmailOtp(secretHasher.hash("654321"), Instant.now().plusSeconds(600));
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));

        User confirmed = userService.confirmEmailOtp(new ConfirmEmailOtpCommand(registrationId, "654321"));

        assertThat(confirmed.getEmailVerifiedAt()).isNotNull();
        assertThat(confirmed.getEmailOtpCodeHash()).isNull();
    }

    @Test
    void confirmEmailOtp_throwsInvalidOtp_whenCodeDoesNotMatch() {
        UUID registrationId = UUID.randomUUID();
        User preRegistered = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(registrationId).build();
        preRegistered.requestEmailOtp(secretHasher.hash("654321"), Instant.now().plusSeconds(600));
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));

        assertThatThrownBy(() -> userService.confirmEmailOtp(new ConfirmEmailOtpCommand(registrationId, "000000")))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void confirmEmailOtp_discardsTheCode_afterTheAttemptAllowanceIsSpent() {
        UUID registrationId = UUID.randomUUID();
        User preRegistered = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(registrationId).build();
        preRegistered.requestEmailOtp(TEST_HASHER.hash("654321"), Instant.now().plusSeconds(600));
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));

        // Four wrong guesses are merely wrong; the fifth spends the allowance.
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> userService.confirmEmailOtp(new ConfirmEmailOtpCommand(registrationId, "000000")))
                    .isInstanceOf(InvalidOtpException.class);
        }
        assertThatThrownBy(() -> userService.confirmEmailOtp(new ConfirmEmailOtpCommand(registrationId, "000000")))
                .isInstanceOf(EmailOtpAttemptsExhaustedException.class);

        // And the real code no longer works, which is the point - otherwise the cap would only
        // change the error message.
        assertThat(preRegistered.getEmailOtpCodeHash()).isNull();
        assertThatThrownBy(() -> userService.confirmEmailOtp(new ConfirmEmailOtpCommand(registrationId, "654321")))
                .isInstanceOf(EmailOtpAttemptsExhaustedException.class);
    }

    @Test
    void requestEmailOtp_refusesOnceTheResendLimitIsReached() {
        UUID registrationId = UUID.randomUUID();
        User preRegistered = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(registrationId).build();
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));

        for (int issued = 1; issued <= 5; issued++) {
            userService.requestEmailOtp(registrationId);
        }

        // The sixth is refused: the resend count is never reset, so this bounds the total number of
        // guesses regardless of how many fresh codes are requested.
        assertThatThrownBy(() -> userService.requestEmailOtp(registrationId))
                .isInstanceOf(EmailOtpResendLimitReachedException.class);
    }

    @Test
    void confirmEmailOtp_throwsInvalidOtp_whenCodeExpired() {
        UUID registrationId = UUID.randomUUID();
        User preRegistered = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(registrationId).build();
        preRegistered.requestEmailOtp(secretHasher.hash("654321"), Instant.now().minusSeconds(1));
        when(userRepository.findById(registrationId)).thenReturn(Optional.of(preRegistered));

        assertThatThrownBy(() -> userService.confirmEmailOtp(new ConfirmEmailOtpCommand(registrationId, "654321")))
                .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void verifyByToken_movesToVerified_whenTokenIsValid() {
        UUID keycloakUserId = UUID.randomUUID();
        User unverified = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(UUID.randomUUID()).build();
        unverified.completeRegistration("Jane Doe");
        unverified.accountProvisioned(
                keycloakUserId, secretHasher.hash("tok-123"), Instant.now().plusSeconds(3600));
        when(userRepository.findByVerificationTokenHash(TEST_HASHER.hash("tok-123"))).thenReturn(Optional.of(unverified));

        User verified = userService.verifyByToken("tok-123");

        assertThat(verified.getRegistrationStatus()).isEqualTo(RegistrationStatus.VERIFIED);
        assertThat(verified.getVerifiedAt()).isNotNull();
        verify(outboxStore, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void verifyByToken_throwsNotFound_whenTokenIsExpired() {
        User unverified = User.preRegister("+91 9800000009", "newcomer@example.com").toBuilder().id(UUID.randomUUID()).build();
        unverified.completeRegistration("Jane Doe");
        unverified.accountProvisioned(
                UUID.randomUUID(), TEST_HASHER.hash("tok-expired"), Instant.now().minusSeconds(1));
        when(userRepository.findByVerificationTokenHash(TEST_HASHER.hash("tok-expired")))
                .thenReturn(Optional.of(unverified));

        assertThatThrownBy(() -> userService.verifyByToken("tok-expired")).isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void getCurrentUser_returnsUser_whenKeycloakUserIdMatches() {
        UUID keycloakUserId = UUID.randomUUID();
        User user = User.createNew("jdoe", "jdoe@example.com", "Jane Doe", null);
        when(userRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(user));

        assertThat(userService.getCurrentUser(keycloakUserId)).isSameAs(user);
    }
}

