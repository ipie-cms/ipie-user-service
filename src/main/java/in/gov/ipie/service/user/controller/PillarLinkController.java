package in.gov.ipie.service.user.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.PathVariable;

import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.common.security.permission.RequiresPermission;
import in.gov.ipie.service.user.mapper.PillarLinkApiMapper;
import in.gov.ipie.service.user.dto.request.InitiatePillarLinkRequest;
import in.gov.ipie.service.user.dto.response.InitiatePillarLinkResponse;
import in.gov.ipie.service.user.dto.response.PillarLinkResponse;
import in.gov.ipie.service.user.permission.UserPermissions;
import in.gov.ipie.service.user.service.PillarLinkService;
import in.gov.ipie.service.user.service.UserService;
import in.gov.ipie.service.user.domain.PillarLinkRequest;

/**
 * Pillar-account linking API (IBBI/NCLT/NCLAT/MCA/NeSL SSO federation) - the explicit
 * initiate/callback handshake an authenticated ipie user drives. Only HTTP concerns live here
 * (master standards doc, 5.1/5.2) - all business rules live in {@link PillarLinkService}.
 *
 * <p>No longer exposes {@code POST .../resolve} - the Keycloak SPI's first-broker-login
 * Authenticator calls ipie-iam-service's own {@code /internal/pillar-links/resolve} instead,
 * reading a local projection rather than hitting this service synchronously on every SSO login
 * (ADR-001, "Placement of pillar_links Data and /resolve Endpoint"). The resolve logic that
 * used to live here ({@code PillarLinkService#resolveLink}) was dead code left behind after
 * that move - removed, not just unused.
 */
@RestController
@RequestMapping("/api/v1/pillar-links")
public class PillarLinkController {

    private final PillarLinkService pillarLinkService;
    private final UserService userService;
    private final PillarLinkApiMapper pillarLinkApiMapper;
    private final CurrentUserProvider currentUserProvider;
    private final String webCallbackResultUrl;

    public PillarLinkController(
            PillarLinkService pillarLinkService,
            UserService userService,
            PillarLinkApiMapper pillarLinkApiMapper,
            CurrentUserProvider currentUserProvider,
            @Value("${ipie.web.base-url:http://localhost:5173}") String webBaseUrl) {
        this.pillarLinkService = pillarLinkService;
        this.userService = userService;
        this.pillarLinkApiMapper = pillarLinkApiMapper;
        this.currentUserProvider = currentUserProvider;
        this.webCallbackResultUrl = webBaseUrl + "/pillar-links/result";
    }

    @PostMapping("/initiate")
    public InitiatePillarLinkResponse initiate(@Valid @RequestBody InitiatePillarLinkRequest request) {
        UUID userId = currentIpieUserId();
        PillarLinkRequest linkRequest = pillarLinkService.initiateLink(userId, request.pillarType());
        String authorizationUrl = pillarLinkService.buildAuthorizationUrl(linkRequest);
        return new InitiatePillarLinkResponse(linkRequest.getId(), authorizationUrl);
    }

    /**
     * Followed from the pillar's own IdP - a plain browser redirect target, not an API
     * call carrying a bearer token (deliberately unauthenticated, see {@code
     * ipie.security.public-paths}, same rationale as {@code RegistrationController}'s Javadoc).
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam UUID state) {
        String status;
        try {
            pillarLinkService.completeLinkCallback(code, state);
            status = "linked";
        } catch (RuntimeException e) {
            status = "failed";
        }
        return ResponseEntity.status(302).location(URI.create(webCallbackResultUrl + "?status=" + status)).build();
    }

    @GetMapping
    public List<PillarLinkResponse> listMyLinks() {
        UUID userId = currentIpieUserId();
        return pillarLinkService.listLinksForUser(userId).stream().map(pillarLinkApiMapper::toResponse).toList();
    }

    /**
     * Admin-facing counterpart to {@link #listMyLinks()} - any user's linked pillar accounts,
     * not just the caller's own. Same permission that already gates viewing the user list/detail
     * itself ({@code UserController}), since this is user-related metadata, not a separate concern.
     */
    @GetMapping("/{userId}")
    @RequiresPermission(UserPermissions.USER_READ)
    public List<PillarLinkResponse> listLinksForUser(@PathVariable UUID userId) {
        return pillarLinkService.listLinksForUser(userId).stream().map(pillarLinkApiMapper::toResponse).toList();
    }

    /**
     * {@code currentUserProvider} only resolves the Keycloak user id (JWT {@code sub});
     * {@code pillar_links.user_id} is ipie-user-service's own id, so this needs one extra hop
     * via {@link UserService}, matching how {@code UserController#getCurrentUser} does the same
     * resolution for {@code GET /api/v1/users/me}.
     */
    private UUID currentIpieUserId() {
        UUID keycloakUserId = UUID.fromString(currentUserProvider.currentOrThrow().userId());
        return userService.getCurrentUser(keycloakUserId).getId();
    }
}
