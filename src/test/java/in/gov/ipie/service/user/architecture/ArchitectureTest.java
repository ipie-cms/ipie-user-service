package in.gov.ipie.service.user.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import in.gov.ipie.common.testing.archunit.LayeredArchitectureRules;

/**
 * Enforces the layering rules every iPIE service must follow (master standards doc, section 16),
 * plus the binding rules that stop a service from re-inventing something ipie-common-libs already
 * provides (master standards doc binding rules, Section 13). A new service created from this
 * template keeps this test unchanged except for the base package.
 */
@AnalyzeClasses(packages = "in.gov.ipie.service.user")
class ArchitectureTest {

    private static final String BASE_PACKAGE = "in.gov.ipie.service.user";

    @ArchTest
    static final ArchRule controllers_do_not_access_repositories_directly =
            LayeredArchitectureRules.controllersDoNotAccessRepositoriesDirectly(BASE_PACKAGE);

    @ArchTest
    static final ArchRule domain_does_not_depend_on_infrastructure =
            LayeredArchitectureRules.domainDoesNotDependOnInfrastructure(BASE_PACKAGE);

    @ArchTest
    static final ArchRule application_does_not_depend_on_api =
            LayeredArchitectureRules.applicationDoesNotDependOnApi(BASE_PACKAGE);

    @ArchTest
    static final ArchRule api_does_not_expose_persistence_entities =
            LayeredArchitectureRules.apiDoesNotExposePersistenceEntities(BASE_PACKAGE);

    @ArchTest
    static final ArchRule exceptions_extend_ipie_exception =
            LayeredArchitectureRules.exceptionsExtendIpieException(BASE_PACKAGE);

    @ArchTest
    static final ArchRule no_competing_controller_advice =
            LayeredArchitectureRules.noCompetingControllerAdvice(BASE_PACKAGE);

    @ArchTest
    static final ArchRule application_does_not_call_event_publisher_directly =
            LayeredArchitectureRules.applicationDoesNotCallEventPublisherDirectly(BASE_PACKAGE);

    /**
     * Added with the 2026-08-18 consolidation: the platform ships the JPA implementation of its own
     * event ports, so a service declares an entity and points the store at it rather than keeping a
     * copy of the adapter.
     */
    @ArchTest
    static final ArchRule no_hand_written_platform_port_adapters =
            LayeredArchitectureRules.noHandWrittenPlatformPortAdapters(BASE_PACKAGE);
}
