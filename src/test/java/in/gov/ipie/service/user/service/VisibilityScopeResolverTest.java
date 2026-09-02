package in.gov.ipie.service.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.common.security.context.CurrentUser;
import in.gov.ipie.common.security.context.CurrentUserProvider;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.VisibilityScope;
import in.gov.ipie.service.user.repository.UserRepository;

class VisibilityScopeResolverTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final VisibilityScopeResolver resolver =
            new VisibilityScopeResolver(currentUserProvider, userRepository);

    private static final UUID KEYCLOAK_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private void callerIs(Set<String> realmRoles, User record) {
        when(currentUserProvider.current())
                .thenReturn(Optional.of(new CurrentUser(KEYCLOAK_ID.toString(), "caller", realmRoles)));
        when(userRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.ofNullable(record));
    }

    private static User user(UUID organisationId, String pillarScope) {
        return User.builder().id(USER_ID).keycloakUserId(KEYCLOAK_ID)
                .organisationId(organisationId).pillarScope(pillarScope).build();
    }

    @Test
    void aSuperAdminIsUnrestricted() {
        callerIs(Set.of("SUPER_ADMIN"), user(null, null));

        assertThat(resolver.forCurrentUser().unrestricted()).isTrue();
    }

    @Test
    void aPillarAdminSeesOnlyTheirOwnPillar() {
        callerIs(Set.of("PILLAR_ADMIN"), user(null, "IBBI"));

        VisibilityScope scope = resolver.forCurrentUser();

        assertThat(scope.unrestricted()).isFalse();
        assertThat(scope.pillarScopes()).containsExactly("IBBI");
        // An IBBI administrator must not acquire NCLT's principals by holding the role alone.
        assertThat(scope.pillarScopes()).doesNotContain("NCLT");
    }

    @Test
    void anEntityAdminIsScopedToTheirOwnNode() {
        // The node only - descendants are expanded by the backend, as a subquery against
        // organisation_closure, so no id list is built here. Sibling isolation follows from the
        // closure containing no ancestor path between two siblings (proved in V12's data).
        UUID childA = UUID.randomUUID();
        UUID childB = UUID.randomUUID();
        callerIs(Set.of(), user(childA, null));

        VisibilityScope scope = resolver.forCurrentUser();

        assertThat(scope.hierarchyRootIds()).containsExactly(childA);
        assertThat(scope.hierarchyRootIds()).doesNotContain(childB);
    }

    @Test
    void bothAxesApplyTogetherWhenTheCallerCarriesBoth() {
        UUID organisation = UUID.randomUUID();
        callerIs(Set.of("PILLAR_ADMIN"), user(organisation, "NCLT"));

        VisibilityScope scope = resolver.forCurrentUser();

        // A union, not an intersection - an insolvency professional carries a pillar and no
        // organisation, so intersecting would hide exactly who a pillar admin exists to administer.
        assertThat(scope.hierarchyRootIds()).containsExactly(organisation);
        assertThat(scope.pillarScopes()).containsExactly("NCLT");
    }

    @Test
    void aPlainUserSeesOnlyThemselves() {
        callerIs(Set.of(), user(null, null));

        VisibilityScope scope = resolver.forCurrentUser();

        assertThat(scope.isSelfOnly()).isTrue();
        assertThat(scope.selfUserId()).isEqualTo(USER_ID);
    }

    @Test
    void holdingPillarAdminWithoutAPillarGrantsNoPillarReach() {
        // The role says "administers a pillar"; which pillar comes from the caller's own record. An
        // unscoped assignment must not read as "all pillars".
        callerIs(Set.of("PILLAR_ADMIN"), user(null, null));

        assertThat(resolver.forCurrentUser().isSelfOnly()).isTrue();
    }
}
