package com.breuninger.homefeed

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * The executable architecture rules from ADR-001 (dependency direction) and
 * ADR-007 (naming). If one of these blocks you, the design is being violated —
 * fix the code, never relax the rule.
 */
@AnalyzeClasses(packages = ["com.breuninger.homefeed"], importOptions = [DoNotIncludeTests::class])
class BngrArchitectureTest {

    @ArchTest
    val `feed domain is framework-free` =
        noClasses().that().resideInAPackage("..feed.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "com.fasterxml.jackson..", "jakarta.persistence..")
            .because("feed/domain is the hexagon core (ADR-001)")

    @ArchTest
    val `feed depends on no other domain` =
        noClasses().that().resideInAPackage("..homefeed.feed..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..homefeed.catalog..", "..homefeed.orders..", "..homefeed.auth..")
            .because("domains plug into the feed contract, never the other way around (ADR-001)")

    @ArchTest
    val `feed domain does not even depend on shared` =
        noClasses().that().resideInAPackage("..feed.domain..")
            .should().dependOnClassesThat().resideInAPackage("..homefeed.shared..")
            .because("the hexagon core stays free of cross-cutting infrastructure; feed.api may use shared constants (ADR-001)")

    @ArchTest
    val `catalog depends on no other domain` =
        noClasses().that().resideInAPackage("..homefeed.catalog..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..homefeed.orders..", "..homefeed.auth..")

    @ArchTest
    val `orders depends on no other domain` =
        noClasses().that().resideInAPackage("..homefeed.orders..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..homefeed.catalog..", "..homefeed.auth..")

    @ArchTest
    val `auth depends only on orders (purchase seeding)` =
        noClasses().that().resideInAPackage("..homefeed.auth..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..homefeed.catalog..")
            .because("auth -> orders is the single allowed inter-domain dependency (ADR-001)")

    @ArchTest
    val `every production type carries the business prefix` =
        classes().that().resideInAPackage("com.breuninger.homefeed..")
            .and().areTopLevelClasses()
            .should().haveSimpleNameStartingWith("Bngr")
            .orShould().haveSimpleNameEndingWith("Kt") // Kotlin file facades for top-level functions
            .because("Bngr* makes our frames instantly recognizable in stack traces and dumps (ADR-007)")
}
