package in.gov.ipie.service.user.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import in.gov.ipie.service.user.domain.RegistrationStatus;
import in.gov.ipie.service.user.domain.UserStatus;

/** The two kinds of principal `users` holds since V13, and the invariant that separates them. */
class UserJpaEntityTest {

    @Test
    void aPersonPrincipalGetsItsDetailRowEvenWithNothingToPutInIt() {
        // A PRE_REGISTRATION draft has a username and an email and nothing else. Creating its person
        // row lazily on first write would leave `is_org` false with no detail - a principal every
        // screen that reads a name renders blank, which reads as missing data rather than as a bug.
        UserJpaEntity draft = new UserJpaEntity(UUID.randomUUID(), "draft", "draft@example.com",
                null, null, UserStatus.ACTIVE, RegistrationStatus.PRE_REGISTRATION);

        assertThat(draft.isOrg()).isFalse();
        assertThat(draft.getPerson()).isNotNull();
        assertThat(draft.getFullName()).isNull();
    }

    @Test
    void anEntityPrincipalIsNamedAfterTheGovernmentIdAndHasNoPersonDetail() {
        UserJpaEntity entity = UserJpaEntity.forEntity("PAN", "AABCU9603R");

        assertThat(entity.getUsername()).isEqualTo("pan-aabcu9603r");
        assertThat(entity.isOrg()).isTrue();
        assertThat(entity.getPerson()).isNull();
        assertThat(entity.getFullName()).isNull();
        assertThat(entity.getRegistrationStatus()).isEqualTo(RegistrationStatus.VERIFIED);
    }

    @Test
    void anEntityPrincipalsEmailCanNeverBeDeliveredTo() {
        // The reserved .invalid domain, so nothing can quietly start mailing an entity principal in
        // place of the person authorised to act for it.
        assertThat(UserJpaEntity.forEntity("CIN", "U74140DL2015PTC123456").getEmail())
                .isEqualTo("cin-u74140dl2015ptc123456@entity.invalid");
    }

    @Test
    void writingPersonDetailOntoAnEntityPrincipalIsRefused() {
        // Not absorbed as a null: `is_org` is a denormalised copy of which detail table holds the
        // row, so a write that would create a person row against an entity is the exact way the two
        // fall out of agreement.
        UserJpaEntity entity = UserJpaEntity.forEntity("CIN", "U74140DL2015PTC123456");

        assertThatThrownBy(() -> entity.setFullName("Not A Person"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("organisations");
        assertThat(entity.getPerson()).isNull();
    }
}
