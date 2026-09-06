package in.gov.ipie.service.user.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import in.gov.ipie.common.testing.archunit.LayeredArchitectureRules;

/**
 * Enforces the layering rules every iPIE service must follow (master standards doc, section 16),
 * plus the binding rules that stop a service from re-inventing something ipie-common-libs already
 * provides (master standards doc binding rules, Section 13). A new service created from the
 * template keeps this test unchanged except for the base package.
 *
 * <p><strong>One binding, not one per rule.</strong> This used to list each rule as its own
 * {@code @ArchTest} field, which meant a rule added to the platform reached a service only when
 * somebody remembered to edit that service's copy - and reached a new service only if somebody
 * remembered to create the file at all. {@code LayeredArchitectureRules.all} makes the rule set
 * the platform's to change: a service picks up new rules with its next platform version bump.
 *
 * <p>To suppress one rule, replace this field with the individual rules you do want and say in a
 * comment which one you dropped and why. That is deliberately more work than adding a rule, and
 * deliberately visible in review.
 */
@AnalyzeClasses(packages = "in.gov.ipie.service.user")
class ArchitectureTest {

    private static final String BASE_PACKAGE = "in.gov.ipie.service.user";

    @ArchTest
    static final ArchRule ipie_layering_and_binding_rules = LayeredArchitectureRules.all(BASE_PACKAGE);
}
