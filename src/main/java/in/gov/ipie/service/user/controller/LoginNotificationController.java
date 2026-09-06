package in.gov.ipie.service.user.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.service.user.dto.request.LoginNotificationRequest;
import in.gov.ipie.service.user.permission.UserPermissions;
import in.gov.ipie.service.user.service.UserService;

/**
 * Called by {@code ipie-keycloak-spi}'s {@code LoginNotificationEventListenerProvider} on every
 * successful Keycloak login (Path A/ROPC included - Keycloak validates credentials and issues
 * tokens directly, with no other call into any ipie backend service during login itself, so this
 * SPI-originated call is the only hook point that can observe a login happening at all). Kept
 * thin and event-publishing only, not synchronously notifying ipie-communication-service itself -
 * "who is this user / what are their opted channels" business logic stays in {@link UserService},
 * where the {@code User} aggregate already lives (see {@code UserService#notifyLogin}), and the
 * actual notification delivery reuses the existing outbox -&gt; broker -&gt; consumer pipeline
 * (see {@code UserEventType#USER_LOGGED_IN}) rather than a new one-off synchronous call all the
 * way to ipie-communication-service.
 *
 * <p>HMAC-verified (see {@code SecurityConfig}, {@code ipie.security.hmac.protected-paths}) on
 * top of the {@code @RequiresPermission} gate below - the same double-gate shape
 * ipie-iam-service's {@code PillarLinkResolveController} already uses for its own
 * SPI-originated internal call.
 */
@RestController
public class LoginNotificationController {

    private final UserService userService;

    public LoginNotificationController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/internal/logins/notify")
    @RequiresPermission(UserPermissions.LOGIN_NOTIFY)
    public ResponseEntity<Void> notify(@Valid @RequestBody LoginNotificationRequest request) {
        userService.notifyLogin(request.keycloakUserId(), request.occurredAt(), request.sourceIp());
        return ResponseEntity.noContent().build();
    }
}
