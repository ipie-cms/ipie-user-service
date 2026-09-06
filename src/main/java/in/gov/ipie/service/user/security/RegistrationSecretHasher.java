package in.gov.ipie.service.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.security.secret.PepperedSecretHasher;
import in.gov.ipie.common.security.secret.SecretHasher;

/**
 * How this service stores the bearer secrets its registration flow issues: the email OTP, the
 * verification token, and the identity-proof number.
 *
 * <p>The algorithm is the platform's ({@link PepperedSecretHasher}); what this class contributes is
 * the decision that these particular secrets need the peppered mode. A six-digit OTP has a search
 * space of one million, so an unkeyed digest of it is reversible by anyone holding the database -
 * the pepper is what makes the stored form unreproducible without a value the database does not
 * hold. See {@link SecretHasher} for when the other mode is correct instead.
 *
 * <p>Kept as a named class rather than injecting {@code SecretHasher} directly, because "which
 * hasher" is a security decision and a bare port at the call site would let the wrong one be wired
 * in without anything looking odd.
 */
@Component
public final class RegistrationSecretHasher {

    private final SecretHasher delegate;

    public RegistrationSecretHasher(@Value("${ipie.security.registration-secret-pepper:}") String pepper) {
        this.delegate = new PepperedSecretHasher(pepper);
    }

    public String hash(String plaintext) {
        return delegate.hash(plaintext);
    }

    public boolean matches(String plaintext, String storedHash) {
        return delegate.matches(plaintext, storedHash);
    }
}
