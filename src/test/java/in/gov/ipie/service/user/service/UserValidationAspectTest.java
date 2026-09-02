package in.gov.ipie.service.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.core.exception.ConflictException;
import in.gov.ipie.service.user.command.CreateRegistrationCommand;
import in.gov.ipie.service.user.command.CreateUserCommand;
import in.gov.ipie.service.user.command.UpdateUserCommand;
import in.gov.ipie.service.user.exception.EmailAlreadyExistsException;
import in.gov.ipie.service.user.exception.MobileNumberAlreadyExistsException;
import in.gov.ipie.service.user.exception.UsernameAlreadyExistsException;
import in.gov.ipie.service.user.repository.UserRepository;

class UserValidationAspectTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserValidationAspect aspect = new UserValidationAspect(userRepository);

    @Test
    void validateCreateUser_throwsConflict_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("jdoe")).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateCreateUser(new CreateUserCommand("jdoe", "jdoe@example.com", "Jane Doe", null, null)))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void validateCreateUser_throwsConflict_whenEmailAlreadyExists() {
        when(userRepository.existsByUsername("jdoe")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("jdoe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateCreateUser(new CreateUserCommand("jdoe", "jdoe@example.com", "Jane Doe", null, null)))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void validateCreateUser_passes_whenUsernameAndEmailAreUnique() {
        assertThatCode(() -> aspect.validateCreateUser(new CreateUserCommand("jdoe", "jdoe@example.com", "Jane Doe", null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateUpdateUser_throwsConflict_whenEmailAlreadyExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", userId)).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateUpdateUser(new UpdateUserCommand(userId, "taken@example.com", "Jane Doe", null, null)))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void validateCreateRegistration_passes_whenEmailAndMobileAreUnique() {
        assertThatCode(() -> aspect.validateCreateRegistration(
                        new CreateRegistrationCommand("+91 9800000009", "newcomer@example.com", null)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCreateRegistration_throwsConflict_whenMobileNumberAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+91 9800000009")).thenReturn(true);

        assertThatThrownBy(() -> aspect.validateCreateRegistration(new CreateRegistrationCommand("+91 9800000009", "x@example.com", null)))
                .isInstanceOf(MobileNumberAlreadyExistsException.class);
    }
}
