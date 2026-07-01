package com.lovart.maildesk.api.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * P2-T07 — Feishu integration is strictly read-only (F-STAGE-04).
 * <p>
 * Lives in {@code maildesk-api} tests so the full production classpath (integration +
 * application + api) is visible without a Maven module cycle. HTTP write-path literals
 * are additionally guarded by {@code FeishuReadOnlySourceTest} in maildesk-integration.
 */
@AnalyzeClasses(packages = "com.lovart.maildesk", importOptions = ImportOption.DoNotIncludeTests.class)
class FeishuReadOnlyArchitectureTest {

    private static final String[] FEISHU_SDK_PACKAGES = {
            "com.lark.oapi..",
            "com.larksuite.."
    };

    @ArchTest
    static final ArchRule business_code_must_not_use_feishu_official_sdk =
            noClasses()
                    .that().resideInAPackage("com.lovart.maildesk..")
                    .should().dependOnClassesThat().resideInAnyPackage(FEISHU_SDK_PACKAGES)
                    .because("Feishu access goes through our read-only FeishuClient adapter, not the official SDK");

    @ArchTest
    static final ArchRule outer_layers_must_not_depend_on_feishu_integration_impl =
            noClasses()
                    .that().resideInAnyPackage("..application..", "..api..", "..worker..")
                    .should().dependOnClassesThat().resideInAPackage("..integration.feishu..")
                    .because("callers must depend on domain.feishu.FeishuClient, not integration.feishu impl classes");
}
