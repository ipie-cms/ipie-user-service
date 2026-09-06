package in.gov.ipie.service.user.command;

import java.util.UUID;

public record ConfirmEmailOtpCommand(UUID registrationId, String code) {
}
