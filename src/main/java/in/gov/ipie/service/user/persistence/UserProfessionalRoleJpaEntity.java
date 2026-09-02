package in.gov.ipie.service.user.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import in.gov.ipie.common.persistence.AuditableJpaEntity;

/**
 * A professional role held by one user, with the credential proving it - one row per role.
 *
 * <p>Replaces the four columns that used to sit on {@code users} and could hold exactly one role
 * (see {@code V5__multiple_professional_roles.sql} for why that could not be right). The unique
 * constraint on (user_id, professional_role_id) is enforced by the database rather than only in
 * code: the same role claimed twice by one person is meaningless, and a duplicate would double-count
 * in every report that groups by role.
 */
@Entity
@Table(name = "user_professional_roles")
public class UserProfessionalRoleJpaEntity extends AuditableJpaEntity {

    // No user_id field here on purpose: the owning side is UserJpaEntity's @JoinColumn, and mapping
    // the same column twice makes Hibernate refuse to build the session factory outright.

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "professional_role_id", nullable = false)
    private UUID professionalRoleId;

    @Column(name = "professional_identification_type_id")
    private UUID professionalIdentificationTypeId;

    @Column(name = "professional_identification_value", length = 50)
    private String professionalIdentificationValue;

    @Column(name = "legal_representative_type_id")
    private UUID legalRepresentativeTypeId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProfessionalRoleId() {
        return professionalRoleId;
    }

    public void setProfessionalRoleId(UUID professionalRoleId) {
        this.professionalRoleId = professionalRoleId;
    }

    public UUID getProfessionalIdentificationTypeId() {
        return professionalIdentificationTypeId;
    }

    public void setProfessionalIdentificationTypeId(UUID professionalIdentificationTypeId) {
        this.professionalIdentificationTypeId = professionalIdentificationTypeId;
    }

    public String getProfessionalIdentificationValue() {
        return professionalIdentificationValue;
    }

    public void setProfessionalIdentificationValue(String professionalIdentificationValue) {
        this.professionalIdentificationValue = professionalIdentificationValue;
    }

    public UUID getLegalRepresentativeTypeId() {
        return legalRepresentativeTypeId;
    }

    public void setLegalRepresentativeTypeId(UUID legalRepresentativeTypeId) {
        this.legalRepresentativeTypeId = legalRepresentativeTypeId;
    }
}
