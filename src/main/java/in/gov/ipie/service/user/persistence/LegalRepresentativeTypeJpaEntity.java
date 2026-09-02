package in.gov.ipie.service.user.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Which kind of legal professional a {@code LEGAL_REPRESENTATIVE} is (Advocate/CA/CS/...) - see {@link LookupJpaEntity}'s Javadoc. */
@Entity
@Table(name = "legal_representative_types")
public class LegalRepresentativeTypeJpaEntity extends LookupJpaEntity {
}
