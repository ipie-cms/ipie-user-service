package in.gov.ipie.service.user.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.events.envelope.EventEnvelope;
import in.gov.ipie.common.events.outbox.OutboxStore;
import in.gov.ipie.common.observability.correlation.LoggingContext;
import in.gov.ipie.service.user.event.AccountLinkedPayload;
import in.gov.ipie.service.user.event.UserEventType;
import in.gov.ipie.service.user.exception.LinkRequestExpiredOrNotFoundException;
import in.gov.ipie.service.user.exception.PillarAlreadyLinkedException;
import in.gov.ipie.service.user.exception.UserNotFoundException;
import in.gov.ipie.service.user.domain.PillarLink;
import in.gov.ipie.service.user.domain.PillarLinkRequest;
import in.gov.ipie.service.user.domain.PillarType;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.repository.PillarLinkRepository;
import in.gov.ipie.service.user.repository.PillarLinkRequestRepository;
import in.gov.ipie.service.user.repository.UserRepository;
import in.gov.ipie.service.user.integration.PillarIdTokenClaims;
import in.gov.ipie.service.user.integration.PillarIdpAdminClient;
import in.gov.ipie.service.user.integration.PillarIdpTokenClient;
import in.gov.ipie.service.user.integration.PillarLinkingProperties;

/**
 * {@link PillarLinkService} implementation (sibling to {@link UserService}): the explicit
 * initiate/callback handshake an already-logged-in ipie user drives to link an external
 * pillar account, and the read-only resolve lookup the Keycloak SPI's first-broker-login
 * Authenticator calls at cold-login time. This service's own database is authoritative for the
 * link (see {@code V10__create_pillar_links.sql}'s header comment) - Keycloak is only ever
 * the SSO mechanism.
 *
 * <p>{@link #initiateLink}'s duplicate-link guard lives in {@code PillarLinkValidationAspect}
 * instead of inline here, since it's a pure check over the method's own arguments.
 * {@link #completeLinkCallback}'s equivalent guard stays inline - it depends on
 * {@code claims.pillarId()}, only available after this method's own OAuth code exchange, so
 * it can't be expressed as {@code @Before} advice over the method's arguments.
 *
 * <p>{@code @RequiredArgsConstructor} (rather than a hand-written constructor, this class's
 * former style) is what keeps this class under Checkstyle's {@code ParameterNumber} limit (max
 * 7) now that outbox publishing needs two more dependencies - same reasoning as {@code User}'s
 * {@code @Builder} choice: the generated constructor is synthesized at compile time, so it never
 * appears as a long parameter list in this source file. Lombok copies each field's own
 * annotation (e.g. {@code @Value} below) onto the corresponding generated constructor parameter.
 */
@Service
@RequiredArgsConstructor
public class PillarLinkServiceImpl implements PillarLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(PillarLinkService.class);

    private final PillarLinkRepository pillarLinkRepository;
    private final PillarLinkRequestRepository pillarLinkRequestRepository;
    private final UserRepository userRepository;
    private final PillarIdpTokenClient pillarIdpTokenClient;
    private final PillarIdpAdminClient pillarIdpAdminClient;
    private final PillarLinkingProperties pillarLinkingProperties;

    @Value("${ipie.pillar.linking.request-ttl:PT10M}")
    private final Duration linkRequestTtl;

    private final OutboxStore outboxStore;

    @Value("${spring.application.name}")
    private final String serviceName;

    /** Step 1 of explicit linking: the caller is already an authenticated ipie user. */
    @Transactional
    @Auditable(
            action = "PILLAR_LINK_INITIATED", entityType = "PILLAR_LINK", entityId = "#result.id",
            eventType = AuditEventType.BUSINESS, newValue = "#result")
    @Override
    public PillarLinkRequest initiateLink(UUID userId, PillarType pillarType) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        PillarLinkRequest request = PillarLinkRequest.createNew(userId, pillarType, Instant.now().plus(linkRequestTtl));
        return pillarLinkRequestRepository.save(request);
    }

    @Override
    public String buildAuthorizationUrl(PillarLinkRequest request) {
        PillarLinkingProperties.Provider provider = pillarLinkingProperties.requireProvider(request.getPillarType());
        String encodedRedirectUri = URLEncoder.encode(provider.getRedirectUri(), StandardCharsets.UTF_8);
        return provider.getAuthorizationUrl()
                + "?client_id=" + provider.getClientId()
                + "&redirect_uri=" + encodedRedirectUri
                // preferred_username comes from the pillar-claims client scope's own
                // mapper (added alongside pillar_id/ipie_id), not the standard "profile"
                // scope - keeps this a single default scope, no extra scope negotiation needed.
                + "&response_type=code&scope=openid&state=" + request.getId();
    }

    /**
     * Step 2: the pillar's own IdP redirected back here after authenticating the user -
     * exchanges the code, records the authoritative link.
     */
    @Override
    @Transactional
    @Auditable(
            action = "PILLAR_LINK_COMPLETED", entityType = "PILLAR_LINK", entityId = "#result.id",
            eventType = AuditEventType.BUSINESS, newValue = "#result")
    public PillarLink completeLinkCallback(String code, UUID state) {
        PillarLinkRequest request = pillarLinkRequestRepository.findById(state)
                .filter(PillarLinkRequest::isPendingAndNotExpired)
                .orElseThrow(LinkRequestExpiredOrNotFoundException::new);

        PillarIdTokenClaims claims = pillarIdpTokenClient.exchangeCodeForClaims(request.getPillarType(), code);

        if (pillarLinkRepository.existsByUserIdAndPillarType(request.getUserId(), request.getPillarType())
                || pillarLinkRepository.findByPillarTypeAndExternalPillarId(
                                request.getPillarType(), claims.pillarId())
                        .isPresent()) {
            throw new PillarAlreadyLinkedException(request.getPillarType());
        }

        PillarLink link = PillarLink.createNew(
                request.getUserId(), request.getPillarType(), claims.pillarId(), claims.preferredUsername());
        PillarLink saved = pillarLinkRepository.save(link);

        request.complete();
        pillarLinkRequestRepository.save(request);

        // Keeps ipie-iam-service's pillar_resolution read projection current (ADR-001) -
        // same outbox transaction as the link write above, so the two can never diverge from a
        // crash between them (only from the async relay/consumer step after, which the
        // reconciliation job backstops).
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getUserId()));
        enqueueAccountLinkedEvent(new AccountLinkedPayload(
                saved.getUserId(), user.getKeycloakUserId(), saved.getPillarType(), saved.getExternalPillarId(),
                true, saved.getLinkedAt()));

        // Best-effort: ipie's own row above is already the authoritative record of the link (see
        // this class's Javadoc) - a a pillar being unreachable, or not yet supporting the
        // write-back at all, must never block the link itself from succeeding.
        if (claims.sub() != null) {
            try {
                pillarIdpAdminClient.writeIpieId(request.getPillarType(), claims.sub(), request.getUserId());
            } catch (RuntimeException e) {
                LOG.warn(
                        "Failed to write ipie_id back to {}'s user {} - the link itself still succeeded",
                        request.getPillarType(), claims.sub(), e);
            }
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PillarLink> listLinksForUser(UUID userId) {
        return pillarLinkRepository.findAllByUserId(userId);
    }

    private void enqueueAccountLinkedEvent(AccountLinkedPayload payload) {
        EventEnvelope<AccountLinkedPayload> event = EventEnvelope.create(
                UserEventType.ACCOUNT_LINKED.name(), UserEventType.CONTRACT_VERSION, serviceName,
                LoggingContext.correlationId(), null, payload);
        outboxStore.save(event);
    }
}
