package in.gov.ipie.service.user.repository;

import java.util.List;

import in.gov.ipie.service.user.domain.LookupCatalogue;
import in.gov.ipie.service.user.domain.LookupOption;

/**
 * Port for reading the registration wizard's lookup catalogues. Returns {@link LookupOption},
 * a domain record - the JPA entity behind each catalogue never crosses this boundary.
 *
 * <p>Added 2026-09-05 to close a dependency-inversion leak: {@code RegistrationLookupServiceImpl}
 * injected four Spring Data repositories and mapped {@code LookupJpaEntity} itself. Its Javadoc
 * called that a deliberate shortcut - "these tables carry no business behavior beyond list the
 * active rows" - and on its own terms that was a fair trade. What made it worth undoing is that it
 * was the application layer holding a persistence type, which is the one shape the layering rules
 * exist to prevent, and it went unnoticed for as long as no rule looked at {@code ..service..}.
 *
 * <p>One method keyed by {@link LookupCatalogue} rather than four {@code findX()} methods: adding
 * a catalogue then costs an enum constant and one map entry in the adapter, instead of a new
 * method on this interface and on every implementation of it.
 */
public interface LookupRepository {

    /** Active rows of one catalogue, in display order. Never null; empty if the catalogue has none. */
    List<LookupOption> findActiveOptions(LookupCatalogue catalogue);
}
