package in.gov.ipie.service.user.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import in.gov.ipie.common.security.context.CurrentUser;
import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.VisibilityScope;
import in.gov.ipie.service.user.repository.UserRepository;

/**
 * Works out what the caller of the current request may see, from their own record rather than from
 * anything they send.
 *
 * <p>The rules, as the programme described them on 2026-08-17:
 *
 * <ul>
 *   <li>A <b>super admin</b> sees and updates everything - {@link VisibilityScope#unrestricted}.
 *   <li>A <b>pillar admin</b> sees every principal their pillar validated. Their own
 *       {@code pillarScope} says which pillar; an IBBI administrator sees IBBI's insolvency
 *       professionals and registered valuers, and nothing of NCLT's.
 *   <li>An <b>entity or IP administrator</b> sees the organisation subtree rooted at their own node.
 *       A parent sees everything beneath it; two children of the same parent see nothing of each
 *       other, because each resolves a subtree that does not contain the other.
 *   <li>Everyone else sees their own record and no more.
 * </ul>
 *
 * <p>THE CALLER'S OWN ROW IS THE SOURCE, NOT THE TOKEN. The token would be one round trip cheaper
 * and is the wrong place to look: `pillarScope` and `organisationId` change when an administrator
 * moves someone, and a token already issued would keep the old answer until it expired. The same
 * reasoning that moved iam's delegation ceiling off the `permissions` claim applies here, and more
 * sharply - this decides which *records* are returned, not merely which endpoint answers.
 *
 * <p>Only the administrative *role* is read from the token, because that is a fact about the
 * session rather than about the person's placement.
 */
@Service
public class VisibilityScopeResolver {

    /**
     * Realm-role names. The `permissions` claim is a realm-role projection (10.3), so an
     * administrative role arrives in it under its own name.
     */
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String PILLAR_ADMIN = "PILLAR_ADMIN";

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    public VisibilityScopeResolver(CurrentUserProvider currentUserProvider,
                                   UserRepository userRepository) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
    }

    public VisibilityScope forCurrentUser() {
        CurrentUser caller = currentUserProvider.current().orElse(null);
        if (caller == null) {
            // No security context means an internal caller - a signed inter-service message or a
            // scheduled job. Those are gated where they enter, and narrowing them to "self" here
            // would break them in a way that reads as missing data rather than as a denial.
            return VisibilityScope.unrestricted(null);
        }
        UUID keycloakUserId = parseUuid(caller.userId());
        User self = keycloakUserId == null ? null : userRepository.findByKeycloakUserId(keycloakUserId).orElse(null);
        UUID selfId = self == null ? null : self.getId();

        if (caller.hasPermission(SUPER_ADMIN)) {
            return VisibilityScope.unrestricted(selfId);
        }
        if (self == null) {
            // Authenticated but with no user record - a service account. It sees no user rows; the
            // endpoints it legitimately needs are gated by permission, not by this scope.
            return VisibilityScope.selfOnly(null);
        }

        Set<String> pillars = caller.hasPermission(PILLAR_ADMIN) && self.getPillarScope() != null
                ? Set.of(self.getPillarScope())
                : Set.of();
        // The caller's own node, not its descendants: expanding it is the backend's job, and the
        // Postgres path does it as a subquery so no id list crosses this boundary. Resolved for any
        // caller who has an organisation rather than only for a named admin role - an entity admin
        // is the root of their own tree and nothing marks them as such; the permission gate on the
        // endpoint is what keeps a non-admin from listing anyone.
        Set<UUID> hierarchyRoots = self.getOrganisationId() == null
                ? Set.of()
                : Set.of(self.getOrganisationId());

        return new VisibilityScope(false, selfId, hierarchyRoots, pillars);
    }

    private static UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException notAUuid) {
            // A subject that is not a UUID belongs to no user row - a service account, say. Returning
            // null narrows the caller to their own record rather than throwing, because an
            // unparseable subject must not turn a visibility decision into a 500.
            return null;
        }
    }
}
