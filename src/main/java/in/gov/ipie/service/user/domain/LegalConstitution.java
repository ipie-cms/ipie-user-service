package in.gov.ipie.service.user.domain;

/**
 * The legal-constitution categories an {@link Organisation} can be registered under - FRS 1.1.1's
 * "Category (*NeSL legal constitution category including few other entries)". Government/
 * regulatory entries (Government, Entity Created by or under a Statute) sit alongside ordinary
 * corporate forms since the same registration flow serves both a private company and a
 * regulatory body affiliating a user.
 */
public enum LegalConstitution {
    PUBLIC_LTD_COMPANY,
    PRIVATE_LTD_COMPANY,
    LLP,
    PROPRIETORSHIP,
    PARTNERSHIP,
    ENTITY_CREATED_BY_OR_UNDER_A_STATUTE,
    TRUST,
    HUF,
    CO_OP_SOCIETY,
    ASSOCIATION_OF_PERSONS,
    GOVERNMENT,
    SELF_HELP_GROUP,
    RESIDENT_INDIVIDUAL,
    NON_RESIDENT_FOREIGN_COMPANY,
    OTHER
}
