package org.springframework.samples.petclinic.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static org.springframework.samples.petclinic.architecture.ArchitectureRules.ROOT_PACKAGE;

/**
 * Test conventions adapted from the Spring Framework and Spring Boot architecture rules,
 * checked against test code only.
 */
@AnalyzeClasses(packages = ROOT_PACKAGE, importOptions = ImportOption.OnlyIncludeTests.class)
class TestConventionsRulesTests {

	@ArchTest
	static final ArchRule testMethodsAreNotPublicOrPrivate = ArchitectureRules.testMethodsAreNotPublicOrPrivate();

	@ArchTest
	static final ArchRule testClassesAreNotPublic = ArchitectureRules.testClassesAreNotPublic();

	@ArchTest
	static final ArchRule testClassesAreNamedTests = ArchitectureRules.testClassesAreNamedTests();

}
