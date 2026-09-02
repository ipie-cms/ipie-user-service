package in.gov.ipie.service.user.domain;

/**
 * Which category of individual is registering (FRS 1.1.1's "Select Category" field on the
 * registration form) - drives which identity-proof documents are expected downstream, though that
 * validation isn't enforced yet (see {@link IdentityProofType}).
 */
public enum AccountCategory {
    INDIAN,
    NRI,
    FOREIGNER
}
