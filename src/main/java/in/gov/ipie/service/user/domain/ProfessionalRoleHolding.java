package in.gov.ipie.service.user.domain;

import java.util.UUID;

import lombok.Builder;

/**
 * One professional role a person holds, together with the credential that proves it.
 *
 * <p>A person may hold several. The FRS is explicit - "Add Professional Role (can select multiple
 * roles)", each with its own Professional Identification Type and Value - and IBBI put it beyond
 * doubt on 13 August 2026: a single account supports every role an Insolvency Professional performs,
 * including IRP, RP, Liquidator and Authorised Representative.
 *
 * <p>The credential belongs to the <em>holding</em> rather than to the person, which is the reason
 * this is a type at all rather than three more fields on {@link User}: the same individual acting as
 * an IP carries an IBBI registration number and acting as a legal representative carries a bar
 * registration number. Neither can stand in for the other, and validating one against the wrong
 * issuing body would pass or fail for the wrong reason.
 *
 * @param roleId               the {@code professional_roles} entry claimed
 * @param identificationTypeId which kind of registration number proves it
 * @param identificationValue  the number itself - validated with the issuing body per role
 *                             (Stage 12), never against the person as a whole
 * @param legalRepresentativeTypeId Advocate, CA or CS. Qualifies {@code LEGAL_REPRESENTATIVE} only,
 *                             and is {@code null} on every other holding.
 */
@Builder(toBuilder = true)
public record ProfessionalRoleHolding(
        UUID roleId,
        UUID identificationTypeId,
        String identificationValue,
        UUID legalRepresentativeTypeId) {
}
