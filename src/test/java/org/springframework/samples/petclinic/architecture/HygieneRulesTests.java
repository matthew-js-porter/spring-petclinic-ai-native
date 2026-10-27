package org.springframework.samples.petclinic.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static org.springframework.samples.petclinic.architecture.ArchitectureRules.ROOT_PACKAGE;

/**
 * Coding conventions adapted from the Spring Framework and Spring Boot architecture
 * rules, checked against main and test code.
 */
@AnalyzeClasses(packages = ROOT_PACKAGE)
class HygieneRulesTests {

	@ArchTest
	static final ArchRule packagesAreFreeOfCycles = ArchitectureRules.packagesAreFreeOfCycles(ROOT_PACKAGE);

	@ArchTest
	static final ArchRule caseConversionUsesLocale = ArchitectureRules.caseConversionUsesLocale();

	@ArchTest
	static final ArchRule streamsUseToList = ArchitectureRules.streamsUseToList();

	@ArchTest
	static final ArchRule urlCodingUsesCharset = ArchitectureRules.urlCodingUsesCharset();

	@ArchTest
	static final ArchRule postProcessorBeanMethodsAreStatic = ArchitectureRules.postProcessorBeanMethodsAreStatic();

	@ArchTest
	static final ArchRule beanMethodsDoNotReturnPrivateTypes = ArchitectureRules.beanMethodsDoNotReturnPrivateTypes();

	@ArchTest
	static final ArchRule noStandardStreams = ArchitectureRules.noStandardStreams();

	@ArchTest
	static final ArchRule packagesAreNullMarked = ArchitectureRules.packagesAreNullMarked();

}
