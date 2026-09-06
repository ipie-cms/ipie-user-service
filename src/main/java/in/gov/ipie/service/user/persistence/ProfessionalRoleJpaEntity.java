package in.gov.ipie.service.user.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** The registration wizard's "Professional Roles" dropdown - see {@link LookupJpaEntity}'s Javadoc. */
@Entity
@Table(name = "professional_roles")
public class ProfessionalRoleJpaEntity extends LookupJpaEntity {
}
