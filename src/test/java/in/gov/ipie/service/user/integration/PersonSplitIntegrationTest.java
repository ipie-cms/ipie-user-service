package in.gov.ipie.service.user.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import in.gov.ipie.common.testing.containers.PostgresIntegrationTest;
import in.gov.ipie.service.user.domain.LegalConstitution;
import in.gov.ipie.service.user.domain.Organisation;
import in.gov.ipie.service.user.domain.OrganisationIdType;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.VisibilityScope;
import in.gov.ipie.service.user.repository.OrganisationRepository;
import in.gov.ipie.service.user.repository.UserRepository;
import in.gov.ipie.common.core.paging.PageRequest;

/**
 * The person/entity principal split (V13), proven against a real PostgreSQL instance rather than
 * argued from the migration text.
 *
 * <p>Every assertion here is about an invariant no single constraint can express - that {@code
 * is_org} agrees with which detail table holds the row, on data the migration produced *and* on rows
 * written afterwards through the repositories. A migration that leaves one principal without its
 * detail produces a row every screen renders as blank, which reads as missing data rather than as a
 * broken model, so it has to fail here instead.
 */
@SpringBootTest
@TestPropertySource(properties = "ipie.security.enabled=false")
class PersonSplitIntegrationTest implements PostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Test
    void everyPersonPrincipalHasDetailAndNoEntityPrincipalDoes() {
        Integer peopleWithoutDetail = jdbc.queryForObject(
                "SELECT count(*) FROM users u WHERE NOT u.is_org"
                        + " AND NOT EXISTS (SELECT 1 FROM person p WHERE p.user_id = u.id)", Integer.class);
        Integer entitiesWithPersonDetail = jdbc.queryForObject(
                "SELECT count(*) FROM person p JOIN users u ON u.id = p.user_id WHERE u.is_org", Integer.class);
        Integer entitiesWithoutOrganisation = jdbc.queryForObject(
                "SELECT count(*) FROM users u WHERE u.is_org"
                        + " AND NOT EXISTS (SELECT 1 FROM organisations o WHERE o.user_id = u.id)", Integer.class);

        assertThat(peopleWithoutDetail).isZero();
        assertThat(entitiesWithPersonDetail).isZero();
        assertThat(entitiesWithoutOrganisation).isZero();
    }

    @Test
    void theSeededPeopleKeptTheirDetailThroughTheMove() {
        // The migration copied rather than re-derived, so a seeded user's name has to survive it -
        // a person table that exists but is empty would satisfy every count above and still have
        // lost the data.
        Integer namedPeople = jdbc.queryForObject(
                "SELECT count(*) FROM person WHERE full_name IS NOT NULL", Integer.class);

        assertThat(namedPeople).isPositive();
    }

    @Test
    void theColumnsThatMovedAreGoneFromUsers() {
        // Left behind, they would be written by nothing and read by something - the failure mode of
        // every half-finished normalisation.
        List<String> remaining = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns"
                        + " WHERE table_schema = 'public' AND table_name = 'users'"
                        + " AND column_name IN ('full_name', 'phone_number', 'category', 'address_line1',"
                        + " 'address_line2', 'country', 'state', 'city', 'pin', 'identity_proof_type_id',"
                        + " 'identity_proof_number_hash', 'identity_proof_number_last4')",
                String.class);

        assertThat(remaining).isEmpty();
    }

    @Test
    void everyOrganisationIsAPrincipalNamedAfterItsGovernmentId() {
        List<String> mismatched = jdbc.queryForList(
                "SELECT u.username FROM organisations o JOIN users u ON u.id = o.user_id"
                        + " WHERE u.username <> lower(o.id_type) || '-' || lower(o.id_value)"
                        + "    OR u.email <> lower(o.id_type) || '-' || lower(o.id_value) || '@entity.invalid'"
                        + "    OR NOT u.is_org",
                String.class);

        assertThat(mismatched).isEmpty();
    }

    @Test
    void theOrganisationQualificationTableIsGoneAndItsRowsSurvived() {
        // V11's organisation_professional_roles existed only because user_professional_roles had a
        // user_id and no counterpart. Folding it back must not lose the IBBI recognition number that
        // was the whole reason it was a table rather than a boolean.
        Integer tableExists = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables"
                        + " WHERE table_schema = 'public' AND table_name = 'organisation_professional_roles'",
                Integer.class);
        Integer entityQualifications = jdbc.queryForObject(
                "SELECT count(*) FROM user_professional_roles upr"
                        + " JOIN users u ON u.id = upr.user_id WHERE u.is_org", Integer.class);

        assertThat(tableExists).isZero();
        assertThat(entityQualifications).isPositive();
    }

    @Test
    void savingAPersonWritesBothRowsInOneTransaction() {
        User saved = userRepository.save(
                User.createNew("split.person", "split.person@example.com", "Split Person", "+91 9800000456"));

        Integer detailRows = jdbc.queryForObject(
                "SELECT count(*) FROM person WHERE user_id = ?", Integer.class, saved.getId());
        Boolean isOrg = jdbc.queryForObject(
                "SELECT is_org FROM users WHERE id = ?", Boolean.class, saved.getId());

        assertThat(detailRows).isEqualTo(1);
        assertThat(isOrg).isFalse();
        assertThat(userRepository.findById(saved.getId()).orElseThrow().getFullName()).isEqualTo("Split Person");
    }

    @Test
    void savingAnOrganisationCreatesItsPrincipalWithIt() {
        // The invariant is enforced where the write happens, so there is no order of calls in which
        // an organisation exists without a principal - not even for one statement.
        Organisation saved = organisationRepository.save(Organisation.builder()
                .name("Split Entity LLP")
                .legalConstitution(LegalConstitution.LLP)
                .idType(OrganisationIdType.PAN)
                .idValue("AABCU9603R")
                .build());

        UUID principalId = jdbc.queryForObject(
                "SELECT user_id FROM organisations WHERE id = ?", UUID.class, saved.getId());

        assertThat(principalId).isNotNull();
        assertThat(jdbc.queryForObject("SELECT username FROM users WHERE id = ?", String.class, principalId))
                .isEqualTo("pan-aabcu9603r");
        assertThat(jdbc.queryForObject("SELECT is_org FROM users WHERE id = ?", Boolean.class, principalId))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM person WHERE user_id = ?", Integer.class, principalId))
                .isZero();
    }

    @Test
    void aUserSearchDoesNotReturnEntityPrincipals() {
        // Entity principals share the table with people since V13. A listing that showed them would
        // put rows named cin-... with an @entity.invalid address in front of an admin, which reads
        // as corrupt data rather than as a deliberate model.
        organisationRepository.save(Organisation.builder()
                .name("Hidden From Search Pvt Ltd")
                .legalConstitution(LegalConstitution.PRIVATE_LTD_COMPANY)
                .idType(OrganisationIdType.CIN)
                .idValue("U74140DL2015PTC987654")
                .build());

        List<User> found = userRepository.search(
                new UserSearchCriteria("u74140dl2015ptc987654", null, null),
                VisibilityScope.unrestricted(UUID.randomUUID()),
                PageRequest.of(0, 50)).content();

        assertThat(found).isEmpty();
    }
}
