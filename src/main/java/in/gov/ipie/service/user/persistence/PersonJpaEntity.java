package in.gov.ipie.service.user.persistence;

import java.util.UUID;

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

import in.gov.ipie.common.persistence.AuditableJpaEntity;
import in.gov.ipie.common.utils.id.UuidV7Generator;
import in.gov.ipie.service.user.domain.AccountCategory;

/**
 * The detail a principal has because it is a person: name, contact, postal address and identity
 * proof (V13). One row per {@link UserJpaEntity} whose {@code isOrg} is false.
 *
 * <p><b>Why this is a table and not more columns on {@code users}.</b> {@code users} held four
 * unrelated things at once - the principal, the person, the postal address and the transient
 * registration workflow - so an entity's principal carried a full name and a phone number it could
 * never have, and a person's carried the shape of a CIN. Splitting the person detail out is the cut
 * the rest depend on (working plan 10.2).
 *
 * <p><b>The reference points this way deliberately.</b> {@code user_id} is UNIQUE NOT NULL here
 * rather than a {@code ref_id} on {@code users} pointing at one of two tables: one column cannot
 * reference two tables, so that direction admits no foreign key at all. This way the database
 * enforces both that every person row has a principal and that no two claim the same one.
 *
 * <p>Never returned from an API and never referenced outside the infrastructure layer (master
 * standards doc, section 16) - {@code UserJpaEntity} reads and writes it, and
 * {@code UserPersistenceMapper} sees only the one {@code User} the two rows make up.
 */
@Entity
@Table(name = "person")
public class PersonJpaEntity extends AuditableJpaEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    /**
     * The principal this detail belongs to. {@code optional = false} because a person row without
     * one is meaningless; the owning side is here, so the foreign key lives on this table.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserJpaEntity user;

    // Nullable for the reason they were nullable on users: unknown until step 2 of self-registration
    // fills the wizard in - see RegistrationStatus.PRE_REGISTRATION.
    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountCategory category;

    @Column(name = "address_line1", length = 500)
    private String addressLine1;

    @Column(name = "address_line2", length = 500)
    private String addressLine2;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String pin;

    /**
     * Which proof was taken, from the {@code identity_proof_types} catalogue - which holds exactly
     * PAN and AADHAAR (V2). The pair therefore already says "PAN, or Aadhaar where there is no PAN";
     * the number itself is never stored, only its hash and a masked last-4.
     */
    @Column(name = "identity_proof_type_id")
    private UUID identityProofTypeId;

    @Column(name = "identity_proof_number_hash", length = 64)
    private String identityProofNumberHash;

    @Column(name = "identity_proof_number_last4", length = 4)
    private String identityProofNumberLast4;

    protected PersonJpaEntity() {
        // required by JPA
    }

    PersonJpaEntity(UserJpaEntity user) {
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public UserJpaEntity getUser() {
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public AccountCategory getCategory() {
        return category;
    }

    public void setCategory(AccountCategory category) {
        this.category = category;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
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

    public UUID getIdentityProofTypeId() {
        return identityProofTypeId;
    }

    public void setIdentityProofTypeId(UUID identityProofTypeId) {
        this.identityProofTypeId = identityProofTypeId;
    }

    public String getIdentityProofNumberHash() {
        return identityProofNumberHash;
    }

    public void setIdentityProofNumberHash(String identityProofNumberHash) {
        this.identityProofNumberHash = identityProofNumberHash;
    }

    public String getIdentityProofNumberLast4() {
        return identityProofNumberLast4;
    }

    public void setIdentityProofNumberLast4(String identityProofNumberLast4) {
        this.identityProofNumberLast4 = identityProofNumberLast4;
    }
}
