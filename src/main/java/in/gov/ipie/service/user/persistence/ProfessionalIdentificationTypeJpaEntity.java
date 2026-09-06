package in.gov.ipie.service.user.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** The registration wizard's "Professional Identification Type" dropdown - see {@link LookupJpaEntity}'s Javadoc. */
@Entity
@Table(name = "professional_identification_types")
public class ProfessionalIdentificationTypeJpaEntity extends LookupJpaEntity {
}
