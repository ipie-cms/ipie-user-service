package in.gov.ipie.service.user.domain;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

import in.gov.ipie.common.core.model.AuditMetadata;

/**
 * The entity/firm a user can optionally be affiliated with (FRS 1.1.1's "Entity") - an
 * Insolvency Professional's registered firm, a creditor's company, a valuer's RVO-registered
 * firm, and so on. Deduplicated by {@code (idType, idValue)}: {@link
 * in.gov.ipie.service.user.service.OrganisationService#findOrCreate} always returns the existing
 * row for a given government id rather than creating a duplicate, per the FRS's "system should
 * ensure that no duplication of entity records happen."
 *
 * <p>Independent of the JPA entity, same reasoning as {@link User}'s Javadoc. {@code @Getter}
 * (JavaBean-style accessors) is what lets {@code OrganisationApiMapper}'s MapStruct interface map
 * this class by property name.
 */
@Getter
@Builder(toBuilder = true)
public final class Organisation {

    private final UUID id;
    private String name;
    private LegalConstitution legalConstitution;
    private final OrganisationIdType idType;
    private final String idValue;
    private boolean msme;
    private String msmeType;
    private String registeredAddress;
    private String contactNumber;
    private String contactEmail;

    /** FRS 1.1.1 Entity Details' Country/State/City/PIN/District - {@code null} until the registration wizard sets them. */
    private String country;
    private String state;
    private String city;
    private String pin;
    private String district;

    private final AuditMetadata auditMetadata;

    // No createNew(...) factory here, unlike User - with this many fields required at creation, a
    // hand-written factory method would itself exceed Checkstyle's ParameterNumber limit (max 7).
    // OrganisationServiceImpl builds new instances via Organisation.builder() directly instead;
    // the generated builder has no such limit since each field is set through its own call. The
    // same reasoning is why updateDetails below stays limited to the pre-existing 7-field set
    // rather than growing further - see updateGeoDetails.

    public void updateDetails(
            String name, LegalConstitution legalConstitution, boolean msme, String msmeType, String registeredAddress,
            String contactNumber, String contactEmail) {
        this.name = name;
        this.legalConstitution = legalConstitution;
        this.msme = msme;
        this.msmeType = msmeType;
        this.registeredAddress = registeredAddress;
        this.contactNumber = contactNumber;
        this.contactEmail = contactEmail;
    }

    /** The registration wizard's Entity Details step - kept separate from {@link #updateDetails} to stay under the 7-param limit. */
    public void updateGeoDetails(String country, String state, String city, String pin, String district) {
        this.country = country;
        this.state = state;
        this.city = city;
        this.pin = pin;
        this.district = district;
    }
}
