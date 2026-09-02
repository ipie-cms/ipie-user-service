package in.gov.ipie.service.user.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** The registration wizard's "Identification ID Type" dropdown - see {@link LookupJpaEntity}'s Javadoc. */
@Entity
@Table(name = "identity_proof_types")
public class IdentityProofTypeJpaEntity extends LookupJpaEntity {
}
