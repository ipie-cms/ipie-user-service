package in.gov.ipie.service.user.dto.request;

/**
 * {@code organisationId} may be {@code null} to un-affiliate (FRS 1.1.1: individual users have
 * none). {@code comment} is the human-supplied reason for the audit trail - optional here (a
 * human-facing UI is expected to require it before submitting, see {@code Auditable}'s Javadoc).
 */
public record AffiliateOrganisationRequest(String organisationId, String comment) {
}
