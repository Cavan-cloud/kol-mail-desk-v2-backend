package com.lovart.maildesk.domain.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Module boundary and ORM guard rules (ADR-006).
 * <p>
 * Runs during {@code maildesk-domain} test phase; scans classes on this module's classpath.
 * Cross-module rules (e.g. controller → mapper) take effect once {@code maildesk-api}
 * controllers land — full-repo scan can be added in {@code maildesk-api} test later.
 */
@AnalyzeClasses(packages = "com.lovart.maildesk")
class ArchitectureTest {

    private static final String[] OUTER_LAYERS = {
            "..infrastructure..",
            "..integration..",
            "..application..",
            "..api..",
            "..worker..",
            "..ai.."
    };

    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(OUTER_LAYERS)
                    .because("domain is the core and must stay free of outer-layer dependencies");

    @ArchTest
    static final ArchRule business_code_must_not_use_jpa_or_hibernate =
            noClasses()
                    .that().resideInAPackage("com.lovart.maildesk..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..",
                            "org.hibernate.."
                    )
                    .because("ORM is MyBatis-Plus only — JPA/Hibernate are forbidden (ADR-006)");

    @ArchTest
    static final ArchRule business_code_must_not_use_jdbc_template =
            noClasses()
                    .that().resideInAPackage("com.lovart.maildesk..")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("org.springframework.jdbc.core.JdbcTemplate")
                    .because("database access goes through MyBatis-Plus mappers, not JdbcTemplate");

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_mappers =
            noClasses()
                    .that().resideInAPackage("..api.controller..")
                    .should().dependOnClassesThat().resideInAPackage("..mapper..")
                    .because("controllers delegate to application services, never mappers directly")
                    .allowEmptyShould(true);
}
