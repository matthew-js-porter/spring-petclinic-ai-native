package org.springframework.samples.petclinic.architecture;

import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArchitectureRules}, following Spring Boot's
 * {@code ArchitectureCheckTests}: each rule is checked against fixtures that break it,
 * asserting the failure report names the offending code, and against fixtures that
 * comply, asserting it passes. Each case is a package under {@link #FIXTURES}, written
 * {@code rule/case}; the fixtures live outside the application's package so component
 * and repository scanning never sees them.
 */
class ArchitectureRulesTests {

	private static final String FIXTURES = "org.springframework.samples.architecture.fixtures";

	@Test
	void whenPackagesAreTangledShouldFail() {
		checkAndFail(ArchitectureRules.packagesAreFreeOfCycles(), prepare("tangled"), "tangled.a.TangledFeature");
	}

	@Test
	void whenPackagesAreNotTangledShouldSucceed() {
		check(ArchitectureRules.packagesAreFreeOfCycles(), prepare("untangled"));
	}

	@Test
	void whenToUpperCaseIsCalledWithoutLocaleShouldFail() {
		checkAndFail(ArchitectureRules.caseConversionUsesLocale(), prepare("string/toUpperCase"),
				"ToUpperCase.convert(java.lang.String)");
	}

	@Test
	void whenToUpperCaseIsCalledWithLocaleShouldSucceed() {
		check(ArchitectureRules.caseConversionUsesLocale(), prepare("string/toUpperCaseWithLocale"));
	}

	@Test
	void whenToLowerCaseIsCalledWithoutLocaleShouldFail() {
		checkAndFail(ArchitectureRules.caseConversionUsesLocale(), prepare("string/toLowerCase"),
				"ToLowerCase.convert(java.lang.String)");
	}

	@Test
	void whenToLowerCaseIsCalledWithLocaleShouldSucceed() {
		check(ArchitectureRules.caseConversionUsesLocale(), prepare("string/toLowerCaseWithLocale"));
	}

	@Test
	void whenCollectorsToListIsCalledShouldFail() {
		checkAndFail(ArchitectureRules.streamsUseToList(), prepare("collectors/toList"),
				"CollectorsToList.collect(java.util.stream.Stream)");
	}

	@Test
	void whenStreamToListIsCalledShouldSucceed() {
		check(ArchitectureRules.streamsUseToList(), prepare("collectors/streamToList"));
	}

	@Test
	void whenUrlEncoderIsCalledWithPlatformCharsetShouldFail() {
		checkAndFail(ArchitectureRules.urlCodingUsesCharset(), prepare("url/encodePlatformCharset"),
				"EncodePlatformCharset.encode(java.lang.String)");
	}

	@Test
	void whenUrlEncoderIsCalledWithCharsetNameShouldFail() {
		checkAndFail(ArchitectureRules.urlCodingUsesCharset(), prepare("url/encodeCharsetName"),
				"EncodeCharsetName.encode(java.lang.String)");
	}

	@Test
	void whenUrlDecoderIsCalledWithPlatformCharsetShouldFail() {
		checkAndFail(ArchitectureRules.urlCodingUsesCharset(), prepare("url/decodePlatformCharset"),
				"DecodePlatformCharset.decode(java.lang.String)");
	}

	@Test
	void whenUrlDecoderIsCalledWithCharsetNameShouldFail() {
		checkAndFail(ArchitectureRules.urlCodingUsesCharset(), prepare("url/decodeCharsetName"),
				"DecodeCharsetName.decode(java.lang.String)");
	}

	@Test
	void whenUrlCodingUsesCharsetShouldSucceed() {
		check(ArchitectureRules.urlCodingUsesCharset(), prepare("url/charset"));
	}

	@Test
	void whenBeanPostProcessorBeanMethodIsNotStaticShouldFail() {
		checkAndFail(ArchitectureRules.postProcessorBeanMethodsAreStaticAndDoNotInitializeEarly(), prepare("bpp/nonstatic"),
				"NonStaticBeanPostProcessorConfiguration.postProcessor()");
	}

	@Test
	void whenBeanPostProcessorBeanMethodIsStaticAndHasUnsafeParametersShouldFail() {
		checkAndFail(ArchitectureRules.postProcessorBeanMethodsAreStaticAndDoNotInitializeEarly(), prepare("bpp/unsafeparameters"),
				"javax.sql.DataSource");
	}

	@Test
	void whenBeanPostProcessorBeanMethodIsStaticAndHasSafeParametersShouldSucceed() {
		check(ArchitectureRules.postProcessorBeanMethodsAreStaticAndDoNotInitializeEarly(),
				prepare("bpp/safeparameters"));
	}

	@Test
	void whenBeanPostProcessorBeanMethodIsStaticAndHasNoParametersShouldSucceed() {
		check(ArchitectureRules.postProcessorBeanMethodsAreStaticAndDoNotInitializeEarly(),
				prepare("bpp/noparameters"));
	}

	@Test
	void whenBeanFactoryPostProcessorBeanMethodIsNotStaticShouldFail() {
		checkAndFail(ArchitectureRules.postProcessorBeanMethodsAreStaticAndDoNotInitializeEarly(), prepare("bfpp/nonstatic"),
				"NonStaticBeanFactoryPostProcessorConfiguration.postProcessor()");
	}

	@Test
	void whenBeanFactoryPostProcessorBeanMethodIsStaticAndHasParametersShouldFail() {
		checkAndFail(ArchitectureRules.postProcessorBeanMethodsAreStaticAndDoNotInitializeEarly(), prepare("bfpp/parameters"),
				"javax.sql.DataSource");
	}

	@Test
	void whenBeanFactoryPostProcessorBeanMethodIsStaticAndHasNoParametersShouldSucceed() {
		check(ArchitectureRules.postProcessorBeanMethodsAreStaticAndDoNotInitializeEarly(),
				prepare("bfpp/noparameters"));
	}

	@Test
	void whenBeanFactoryPostProcessorBeanMethodIsStaticAndInjectsEnvironmentShouldSucceed() {
		check(ArchitectureRules.postProcessorBeanMethodsAreStaticAndDoNotInitializeEarly(),
				prepare("bfpp/environment"));
	}

	@Test
	void whenBeanMethodReturnsPrivateTypeShouldFail() {
		checkAndFail(ArchitectureRules.beanMethodsDoNotReturnPrivateTypes(), prepare("beans/privatebean"),
				"PrivateBeanConfiguration.hidden()");
	}

	@Test
	void whenBeanMethodReturnsPackagePrivateTypeShouldSucceed() {
		check(ArchitectureRules.beanMethodsDoNotReturnPrivateTypes(), prepare("beans/regular"));
	}

	@Test
	void whenSystemOutIsUsedShouldFail() {
		checkAndFail(ArchitectureRules.noStandardStreams(), prepare("streams/systemOut"),
				"SystemOut.print(java.lang.String)");
	}

	@Test
	void whenSystemErrIsUsedShouldFail() {
		checkAndFail(ArchitectureRules.noStandardStreams(), prepare("streams/systemErr"),
				"SystemErr.print(java.lang.String)");
	}

	@Test
	void whenPrintStackTraceIsCalledShouldFail() {
		checkAndFail(ArchitectureRules.noStandardStreams(), prepare("streams/printStackTrace"),
				"PrintStackTrace.report(java.lang.Exception)");
	}

	@Test
	void whenLoggerIsUsedShouldSucceed() {
		check(ArchitectureRules.noStandardStreams(), prepare("streams/logger"));
	}

	@Test
	void whenPackageIsNotNullMarkedShouldFail() {
		checkAndFail(ArchitectureRules.packagesAreNullMarked(), prepare("nullmarked/notannotated"),
				"nullmarked.notannotated.NotAnnotated");
	}

	@Test
	void whenPackageIsNullMarkedShouldSucceed() {
		check(ArchitectureRules.packagesAreNullMarked(), prepare("nullmarked/annotated"));
	}

	@Test
	void whenTestMethodIsPublicShouldFail() {
		checkAndFail(ArchitectureRules.testMethodsAreNotPublicOrPrivate(), prepare("testconventions/publicMethod"),
				"PublicMethodTests.run()");
	}

	@Test
	void whenTestMethodIsPrivateShouldFail() {
		checkAndFail(ArchitectureRules.testMethodsAreNotPublicOrPrivate(), prepare("testconventions/privateMethod"),
				"PrivateMethodTests.run()");
	}

	@Test
	void whenTestMethodIsPackagePrivateShouldSucceed() {
		check(ArchitectureRules.testMethodsAreNotPublicOrPrivate(), prepare("testconventions/compliant"));
	}

	@Test
	void whenTestClassIsPublicShouldFail() {
		checkAndFail(ArchitectureRules.testClassesAreNotPublic(), prepare("testconventions/publicClass"),
				"testconventions.publicClass.PublicClassTests");
	}

	@Test
	void whenTestClassIsPackagePrivateShouldSucceed() {
		check(ArchitectureRules.testClassesAreNotPublic(), prepare("testconventions/compliant"));
	}

	@Test
	void whenTestClassIsNotNamedTestsShouldFail() {
		checkAndFail(ArchitectureRules.testClassesAreNamedTests(), prepare("testconventions/misnamed"),
				"testconventions.misnamed.MisnamedTestHolder");
	}

	@Test
	void whenTestClassIsNamedTestsShouldSucceed() {
		check(ArchitectureRules.testClassesAreNamedTests(), prepare("testconventions/compliant"));
	}

	@Test
	void whenRepositoryUsesControllerShouldFail() {
		checkAndFail(ArchitectureRules.layersAreRespected(), prepare("layers/respected", "layers/repositoryUsesController"),
				"repositoryUsesController.LeakyRepository");
	}

	@Test
	void whenPresentationTypeUsesRepositoryShouldFail() {
		checkAndFail(ArchitectureRules.layersAreRespected(), prepare("layers/respected", "layers/viewUsesRepository"),
				"viewUsesRepository.LeakyView");
	}

	@Test
	void whenLayersAreRespectedShouldSucceed() {
		check(ArchitectureRules.layersAreRespected(), prepare("layers/respected"));
	}

	@Test
	void whenRepositoryDependsOnWebTypeShouldFail() {
		checkAndFail(ArchitectureRules.domainAndRepositoriesDoNotDependOnWeb(), prepare("layers/respected", "layers/repositoryUsesController"),
				"repositoryUsesController.LeakyRepository");
	}

	@Test
	void whenDomainAndRepositoriesDoNotDependOnWebTypesShouldSucceed() {
		check(ArchitectureRules.domainAndRepositoriesDoNotDependOnWeb(), prepare("layers/respected"));
	}

	@Test
	void whenRepositoryUsesServletApiShouldFail() {
		checkAndFail(ArchitectureRules.domainAndRepositoriesDoNotUseWebApis(), prepare("layers/repositoryUsesServlet"),
				"repositoryUsesServlet.ServletRepository");
	}

	@Test
	void whenDomainAndRepositoriesDoNotUseWebApisShouldSucceed() {
		check(ArchitectureRules.domainAndRepositoriesDoNotUseWebApis(), prepare("layers/respected"));
	}

	@Test
	void whenFeatureDependsOnAnotherFeatureShouldFail() {
		checkAndFail(ArchitectureRules.featuresDoNotDependOnEachOther(fixture("features/dependent")), prepare("features/dependent"),
				"features.dependent.a.DependentA");
	}

	@Test
	void whenFeaturesAreIndependentShouldSucceed() {
		check(ArchitectureRules.featuresDoNotDependOnEachOther(fixture("features/independent")),
				prepare("features/independent"));
	}

	@Test
	void whenFeatureDependsOnRootPackageShouldFail() {
		checkAndFail(ArchitectureRules.featuresDoNotDependOnRootPackage(fixture("features/rootUser")), prepare("features/rootUser"),
				"features.rootUser.a.RootUser");
	}

	@Test
	void whenOnlyRootPackageDependsOnFeaturesShouldSucceed() {
		check(ArchitectureRules.featuresDoNotDependOnRootPackage(fixture("features/independent")),
				prepare("features/independent"));
	}

	@Test
	void whenClassDependsOnApplicationClassShouldFail() {
		checkAndFail(ArchitectureRules.applicationClassIsNotADependency(), prepare("application/applicationUser"),
				"applicationUser.ApplicationUser");
	}

	@Test
	void whenNoClassDependsOnApplicationClassShouldSucceed() {
		check(ArchitectureRules.applicationClassIsNotADependency(), prepare("application/applicationFree"));
	}

	@Test
	void whenFieldIsAutowiredShouldFail() {
		checkAndFail(ArchitectureRules.noFieldInjection(), prepare("injection/autowiredField"),
				"AutowiredField.collaborator");
	}

	@Test
	void whenCollaboratorIsConstructorInjectedShouldSucceed() {
		check(ArchitectureRules.noFieldInjection(), prepare("injection/constructor"));
	}

	@Test
	void whenControllerIsInWebSubPackageShouldFail() {
		checkAndFail(ArchitectureRules.controllersLiveInRootOrFeaturePackage(fixture("controllers/nested")), prepare("controllers/nested"),
				"feature.web.NestedController");
	}

	@Test
	void whenControllersAreInRootOrFeaturePackageShouldSucceed() {
		check(ArchitectureRules.controllersLiveInRootOrFeaturePackage(fixture("controllers/placed")),
				prepare("controllers/placed"));
	}

	@Test
	void whenControllerIsNotNamedControllerShouldFail() {
		checkAndFail(ArchitectureRules.controllersAreNamedController(), prepare("controllers/unnamed"),
				"unnamed.Greeting");
	}

	@Test
	void whenControllersAreNamedControllerShouldSucceed() {
		check(ArchitectureRules.controllersAreNamedController(), prepare("controllers/placed"));
	}

	@Test
	void whenControllerNamedTypeHasNoStereotypeShouldFail() {
		checkAndFail(ArchitectureRules.controllerNamedTypesAreControllers(), prepare("controllers/plain"),
				"plain.PlainController");
	}

	@Test
	void whenControllerNamedTypesHaveStereotypeShouldSucceed() {
		check(ArchitectureRules.controllerNamedTypesAreControllers(), prepare("controllers/placed"));
	}

	@Test
	void whenControllerIsPublicShouldFail() {
		checkAndFail(ArchitectureRules.controllersAreNotPublic(), prepare("controllers/publicController"),
				"publicController.PublicController");
	}

	@Test
	void whenControllersArePackagePrivateShouldSucceed() {
		check(ArchitectureRules.controllersAreNotPublic(), prepare("controllers/placed"));
	}

	@Test
	void whenRepositoryIsNotNamedRepositoryShouldFail() {
		checkAndFail(ArchitectureRules.repositoriesAreNamedRepository(), prepare("repositories/unnamed"),
				"unnamed.Store");
	}

	@Test
	void whenRepositoryIsNamedRepositoryShouldSucceed() {
		check(ArchitectureRules.repositoriesAreNamedRepository(), prepare("repositories/named"));
	}

	@Test
	void whenRepositoryNamedTypeIsClassShouldFail() {
		checkAndFail(ArchitectureRules.repositoryNamedTypesAreRepositories(), prepare("repositories/classNamedRepository"),
				"classNamedRepository.InMemoryRepository");
	}

	@Test
	void whenRepositoryNamedTypeIsRepositoryInterfaceShouldSucceed() {
		check(ArchitectureRules.repositoryNamedTypesAreRepositories(), prepare("repositories/named"));
	}

	@Test
	void whenDomainTypeIsNamedLikeViewShouldFail() {
		checkAndFail(ArchitectureRules.domainTypesAreNotOtherRoles(), prepare("domain/namedLikeView"),
				"namedLikeView.EntityView");
	}

	@Test
	void whenDomainTypeIsNamedForEntityShouldSucceed() {
		check(ArchitectureRules.domainTypesAreNotOtherRoles(), prepare("domain/entity"));
	}

	@Test
	void whenFormIsRegisteredAsBeanShouldFail() {
		checkAndFail(ArchitectureRules.presentationTypesAreNotBeansOrEntities(), prepare("presentation/componentForm"),
				"componentForm.ComponentForm");
	}

	@Test
	void whenFormIsPlainRecordShouldSucceed() {
		check(ArchitectureRules.presentationTypesAreNotBeansOrEntities(), prepare("presentation/plainForm"));
	}

	/** Imports the classes of the given fixture packages and their sub-packages. */
	private static JavaClasses prepare(String... fixtures) {
		return new ClassFileImporter()
			.importPackages(Stream.of(fixtures).map(ArchitectureRulesTests::fixture).toList());
	}

	private static String fixture(String fixture) {
		return FIXTURES + "." + fixture.replace('/', '.');
	}

	private static void check(ArchRule rule, JavaClasses classes) {
		EvaluationResult result = rule.evaluate(classes);
		assertThat(result.hasViolation()).as("Expected rule to pass but it failed%n%s", result.getFailureReport())
			.isFalse();
	}

	private static void checkAndFail(ArchRule rule, JavaClasses classes, String... messages) {
		EvaluationResult result = rule.evaluate(classes);
		assertThat(result.hasViolation()).as("Expected rule to fail but it passed: %s", rule.getDescription())
			.isTrue();
		assertThat(result.getFailureReport().toString()).contains(rule.getDescription()).contains(messages);
	}

}
