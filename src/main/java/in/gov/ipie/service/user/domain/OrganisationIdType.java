package in.gov.ipie.service.user.domain;

/**
 * The unique-identifier type an {@link Organisation} is deduplicated by - FRS 1.1.1's "Entity
 * Unique ID Type (CIN/PAN/LLPIN/TAN/Other ID)". Paired with the id value itself, this is what
 * {@code OrganisationService#findOrCreate} looks up by, and what the database's
 * {@code UNIQUE (id_type, id_value)} constraint enforces - "system should ensure that no
 * duplication of entity records happen."
 */
public enum OrganisationIdType {
    CIN,
    PAN,
    LLPIN,
    TAN,
    OTHER
}
