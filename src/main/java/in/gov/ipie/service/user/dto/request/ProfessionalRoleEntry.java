package in.gov.ipie.service.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One professional role claimed during registration, with the credential proving it.
 *
 * <p>The FRS asks for a Professional Identification Type and Value <em>for each role selected</em>,
 * which is why they travel together rather than as parallel lists: parallel lists can be sent at
 * different lengths, and the request would then have to decide which credential belonged to which
 * role, or refuse. Pairing them makes the mismatch unrepresentable.
 *
 * @param roleId                    a {@code professional_roles} id
 * @param identificationTypeId      a {@code professional_identification_types} id
 * @param identificationValue       the registration number itself; validated with the issuing body
 *                                  at Stage 12 - IBBI for IP and RV, the bar council or institute
 *                                  for a legal representative
 * @param legalRepresentativeTypeId Advocate, CA or CS. Meaningful only when {@code roleId} is
 *                                  LEGAL_REPRESENTATIVE, and rejected on any other role by
 *                                  {@code UserServiceImpl}, which is the only layer that knows what
 *                                  the id refers to
 */
public record ProfessionalRoleEntry(

        @NotBlank
        String roleId,

        @NotBlank
        String identificationTypeId,

        @NotBlank
        @Size(max = 50)
        String identificationValue,

        String legalRepresentativeTypeId) {
}
