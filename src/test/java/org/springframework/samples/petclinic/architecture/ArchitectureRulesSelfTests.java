package org.springframework.samples.petclinic.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs each architecture rule against fixtures that break it, so a rule that stops
 * matching (after a rename or an ArchUnit upgrade) fails here instead of passing
 * silently. The fixtures live outside the application's package so component and
 * repository scanning never sees them; the application's own code is the passing case,
 * checked by {@link HygieneRulesTests} and {@link LayeringRulesTests}.
 */
class ArchitectureRulesSelfTests {

	private static final String FIXTURES = "org.springframework.samples.architecture.fixtures";

	@Test
	void packageCycleThroughTheRootPackageIsReported() {
		String root = FIXTURES + ".cycles";
		assertViolation(ArchitectureRules.packagesAreFreeOfCycles(root), root, "cycles.a.CycleFeature");
	}

	@Test
	void caseConversionWithoutLocaleIsReported() {
		assertViolation(ArchitectureRules.caseConversionUsesLocale(), "hygiene", "DefaultLocaleCase");
	}

	@Test
	void collectorsToListIsReported() {
		assertViolation(ArchitectureRules.streamsUseToList(), "hygiene", "CollectorsToList");
	}

	@Test
	void urlEncodingWithoutCharsetIsReported() {
		assertViolation(ArchitectureRules.urlCodingUsesCharset(), "hygiene", "PlatformCharsetUrl");
	}

	@Test
	void nonStaticPostProcessorBeanMethodIsReported() {
		assertViolation(ArchitectureRules.postProcessorBeanMethodsAreStatic(), "hygiene",
				"NonStaticPostProcessorBean");
	}

	@Test
	void beanMethodReturningPrivateTypeIsReported() {
		assertViolation(ArchitectureRules.beanMethodsDoNotReturnPrivateTypes(), "hygiene", "PrivateBeanType");
	}

	@Test
	void standardStreamUseIsReported() {
		assertViolation(ArchitectureRules.noStandardStreams(), "hygiene", "StandardStreams");
	}

	@Test
	void packageWithoutNullMarkedIsReported() {
		assertViolation(ArchitectureRules.packagesAreNullMarked(), "unmarked", "Unmarked");
	}

	@Test
	void repositoryReachingUpIsReported() {
		assertViolation(ArchitectureRules.layersAreRespected(), "layers", "LeakyRepository");
	}

	@Test
	void presentationTypeUsingRepositoryIsReported() {
		assertViolation(ArchitectureRules.layersAreRespected(), "layers", "LeakyView");
	}

	@Test
	void repositoryDependingOnWebIsReported() {
		assertViolation(ArchitectureRules.domainAndRepositoriesDoNotDependOnWeb(), "layers", "LeakyRepository");
	}

	@Test
	void repositoryUsingServletApiIsReported() {
		assertViolation(ArchitectureRules.domainAndRepositoriesDoNotUseWebApis(), "layers", "ServletRepository");
	}

	@Test
	void featureDependingOnAnotherFeatureIsReported() {
		String root = FIXTURES + ".features";
		assertViolation(ArchitectureRules.featuresDoNotDependOnEachOther(root), root, "features.a.FeatureA");
	}

	@Test
	void featureDependingOnRootPackageIsReported() {
		String root = FIXTURES + ".features";
		assertViolation(ArchitectureRules.featuresDoNotDependOnRootPackage(root), root, "features.b.FeatureB");
	}

	@Test
	void dependencyOnApplicationClassIsReported() {
		assertViolation(ArchitectureRules.applicationClassIsNotADependency(), "injection", "ApplicationUser");
	}

	@Test
	void fieldInjectionIsReported() {
		assertViolation(ArchitectureRules.noFieldInjection(), "injection", "FieldInjected");
	}

	@Test
	void controllerInWebSubPackageIsReported() {
		String root = FIXTURES + ".stereotypes";
		assertViolation(ArchitectureRules.controllersLiveInRootOrFeaturePackage(root), root,
				"stereotypes.feature.web.DeepController");
	}

	@Test
	void controllerWithoutSuffixIsReported() {
		assertViolation(ArchitectureRules.controllersAreNamedController(), "stereotypes", "Greeting");
	}

	@Test
	void controllerSuffixWithoutStereotypeIsReported() {
		assertViolation(ArchitectureRules.controllerNamedTypesAreControllers(), "stereotypes", "PlainController");
	}

	@Test
	void repositoryWithoutSuffixIsReported() {
		assertViolation(ArchitectureRules.repositoriesAreNamedRepository(), "stereotypes", "Store");
	}

	@Test
	void repositorySuffixOnClassIsReported() {
		assertViolation(ArchitectureRules.repositoryNamedTypesAreRepositories(), "stereotypes", "InMemoryRepository");
	}

	@Test
	void entityNamedLikeViewIsReported() {
		assertViolation(ArchitectureRules.domainTypesAreNotOtherRoles(), "stereotypes", "EntityView");
	}

	@Test
	void formRegisteredAsBeanIsReported() {
		assertViolation(ArchitectureRules.presentationTypesAreNotBeansOrEntities(), "stereotypes", "ComponentForm");
	}

	@Test
	void publicControllerIsReported() {
		assertViolation(ArchitectureRules.controllersAreNotPublic(), "stereotypes", "PublicController");
	}

	@Test
	void publicTestMethodIsReported() {
		assertViolation(ArchitectureRules.testMethodsAreNotPublicOrPrivate(), "testconventions", "PublicTestHolder");
	}

	@Test
	void publicTestClassIsReported() {
		assertViolation(ArchitectureRules.testClassesAreNotPublic(), "testconventions", "PublicTestHolder");
	}

	@Test
	void testClassWithoutTestsSuffixIsReported() {
		assertViolation(ArchitectureRules.testClassesAreNamedTests(), "testconventions", "PublicTestHolder");
	}

	/**
	 * Evaluates the rule against a fixture package (absolute, or relative to
	 * {@link #FIXTURES}) and asserts a violation names the offending class.
	 */
	private static void assertViolation(ArchRule rule, String fixturePackage, String offendingClass) {
		String packageName = fixturePackage.startsWith(FIXTURES) ? fixturePackage : FIXTURES + "." + fixturePackage;
		String className = (offendingClass.contains(".") ? FIXTURES + "." : packageName + ".") + offendingClass;
		EvaluationResult result = rule.evaluate(new ClassFileImporter().importPackages(packageName));
		assertThat(result.hasViolation()).as(rule.getDescription()).isTrue();
		assertThat(result.getFailureReport().getDetails()).as(rule.getDescription())
			.anyMatch((detail) -> detail.contains(className));
	}

}
