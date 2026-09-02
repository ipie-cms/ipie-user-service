package in.gov.ipie.service.user.dto.response;

/** One professional role held, as returned to a client. Ids are strings for the same reason every
 *  other id in this API is: a client should pass them back untouched, not parse them. */
public record ProfessionalRoleResponse(
        String roleId,
        String identificationTypeId,
        String identificationValue,
        String legalRepresentativeTypeId) {
}
