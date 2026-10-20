package io.github.sumeetg13.validate4j.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.github.sumeetg13.validate4j", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule coreIsFrameworkFree = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "javax..")
            .because("validate4j-core must stay usable outside Spring");

    @ArchTest
    static final ArchRule onlyConfigTalksToSnakeYaml = noClasses()
            .that().resideOutsideOfPackage("io.github.sumeetg13.validate4j.config")
            .should().dependOnClassesThat().resideInAPackage("org.yaml..")
            .because("the YAML parser is an implementation detail of the loader");
}
