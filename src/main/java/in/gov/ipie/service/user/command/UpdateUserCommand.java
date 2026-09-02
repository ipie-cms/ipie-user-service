package in.gov.ipie.service.user.command;

import java.util.UUID;

public record UpdateUserCommand(UUID userId, String email, String fullName, String phoneNumber, String comment) {
}

