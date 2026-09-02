package in.gov.ipie.service.user.persistence;

import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import in.gov.ipie.common.utils.id.UuidV7Generator;

import in.gov.ipie.common.persistence.AuditableJpaEntity;
import in.gov.ipie.service.user.domain.LegalConstitution;
import in.gov.ipie.service.user.domain.OrganisationIdType;

/**
 * JPA representation of an organisation. Never returned from an API and never referenced outside
 * the infrastructure layer (master standards doc, section 16) - {@code
 * OrganisationPersistenceMapper} converts to/from the domain {@code Organisation} at the
 * repository boundary. Standard audit + soft-delete columns are inherited from {@link
 * AuditableJpaEntity}.
 */
@Entity
@Table(name = "organisations")
public class OrganisationJpaEntity extends AuditableJpaEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    /**
     * The principal this entity is - "each entity creates a user irrespective of the authorised
     * representative of that entity, so two users get created for an entity" (working plan 10.2).
     * UNIQUE NOT NULL, so no two organisations can claim one principal.
     *
     * <p>Cascaded on persist only. The principal outlives nothing here: removing an organisation
     * must not remove the users row other tables point at, and an update to entity detail is not an
     * update to the principal.
     */
    @OneToOne(cascade = CascadeType.PERSIST, optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserJpaEntity principal;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_constitution", nullable = false, length = 40)
    private LegalConstitution legalConstitution;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", nullable = false, length = 10)
    private OrganisationIdType idType;

    @Column(name = "id_value", nullable = false, length = 50)
    private String idValue;

    @Column(nullable = false)
    private boolean msme;

    @Column(name = "msme_type", length = 50)
    private String msmeType;

    @Column(name = "registered_address", length = 500)
    private String registeredAddress;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "contact_email", length = 254)
    private String contactEmail;

    // FRS 1.1.1 Entity Details' Country/State/City/PIN/District - nullable, set by the
    // registration wizard's Entity Details step (see Organisation.updateGeoDetails).
    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String pin;

    @Column(length = 100)
    private String district;

    protected OrganisationJpaEntity() {
        // required by JPA
    }

    public OrganisationJpaEntity(
            UUID id, String name, LegalConstitution legalConstitution, OrganisationIdType idType, String idValue) {
        this.id = id;
        this.name = name;
        this.legalConstitution = legalConstitution;
        this.idType = idType;
        this.idValue = idValue;
        // Created here rather than by the caller: an entity IS a principal (V13), and `users.is_org`
        // is a denormalised copy of which detail table holds the row, so it has to be written in the
        // same transaction as this one. Building it in the constructor means there is no order of
        // calls in which an organisation exists without its principal.
        this.principal = UserJpaEntity.forEntity(idType.name(), idValue);
    }

    public UserJpaEntity getPrincipal() {
        return principal;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LegalConstitution getLegalConstitution() {
        return legalConstitution;
    }

    public void setLegalConstitution(LegalConstitution legalConstitution) {
        this.legalConstitution = legalConstitution;
    }

    public OrganisationIdType getIdType() {
        return idType;
    }

    public String getIdValue() {
        return idValue;
    }

    public boolean isMsme() {
        return msme;
    }

    public void setMsme(boolean msme) {
        this.msme = msme;
    }

    public String getMsmeType() {
        return msmeType;
    }

    public void setMsmeType(String msmeType) {
        this.msmeType = msmeType;
    }

    public String getRegisteredAddress() {
        return registeredAddress;
    }

    public void setRegisteredAddress(String registeredAddress) {
        this.registeredAddress = registeredAddress;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }
}
