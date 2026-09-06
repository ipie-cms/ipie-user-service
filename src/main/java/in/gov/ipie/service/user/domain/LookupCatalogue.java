package in.gov.ipie.service.user.domain;

/**
 * The registration wizard's database-backed dropdown catalogues, named as a closed set so the
 * application layer can ask for one without knowing which table backs it (see {@code
 * LookupJpaEntity}'s Javadoc for why these are tables and not enums).
 *
 * <p>Note what this enum is and is not. The <em>options</em> inside a catalogue stay data - adding
 * a professional role is still a seed-data insert, never a deploy, which is the whole point of
 * those tables. The <em>catalogues themselves</em> are a closed set: each one exists because a
 * specific wizard field and a specific FK column reference it, so a new catalogue is a schema
 * change and a code change regardless. Naming them here costs nothing that was previously free.
 */
public enum LookupCatalogue {

    PROFESSIONAL_ROLE,
    LEGAL_REPRESENTATIVE_TYPE,
    PROFESSIONAL_IDENTIFICATION_TYPE,
    IDENTITY_PROOF_TYPE
}
