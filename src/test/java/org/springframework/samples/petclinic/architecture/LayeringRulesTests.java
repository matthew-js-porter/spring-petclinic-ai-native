package org.springframework.samples.petclinic.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static org.springframework.samples.petclinic.architecture.ArchitectureRules.ROOT_PACKAGE;

/**
 * The application's layering, feature boundaries and stereotype conventions, checked
 * against main code only.
 */
@AnalyzeClasses(packages = ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringRulesTests {

	@ArchTest
	static final ArchRule layersAreRespected = ArchitectureRules.layersAreRespected();

	@ArchTest
	static final ArchRule domainAndRepositoriesDoNotDependOnWeb = ArchitectureRules
		.domainAndRepositoriesDoNotDependOnWeb();

	@ArchTest
	static final ArchRule featuresDoNotDependOnEachOther = ArchitectureRules
		.featuresDoNotDependOnEachOther(ROOT_PACKAGE);

	@ArchTest
	static final ArchRule featuresDoNotDependOnRootPackage = ArchitectureRules
		.featuresDoNotDependOnRootPackage(ROOT_PACKAGE);

	@ArchTest
	static final ArchRule applicationClassIsNotADependency = ArchitectureRules.applicationClassIsNotADependency();

	@ArchTest
	static final ArchRule noFieldInjection = ArchitectureRules.noFieldInjection();

	@ArchTest
	static final ArchRule controllersLiveInRootOrFeaturePackage = ArchitectureRules
		.controllersLiveInRootOrFeaturePackage(ROOT_PACKAGE);

	@ArchTest
	static final ArchRule controllersAreNamedController = ArchitectureRules.controllersAreNamedController();

	@ArchTest
	static final ArchRule controllerNamedTypesAreControllers = ArchitectureRules.controllerNamedTypesAreControllers();

	@ArchTest
	static final ArchRule repositoriesAreNamedRepository = ArchitectureRules.repositoriesAreNamedRepository();

	@ArchTest
	static final ArchRule repositoryNamedTypesAreRepositories = ArchitectureRules
		.repositoryNamedTypesAreRepositories();

	@ArchTest
	static final ArchRule domainTypesAreNotOtherRoles = ArchitectureRules.domainTypesAreNotOtherRoles();

	@ArchTest
	static final ArchRule presentationTypesAreNotBeansOrEntities = ArchitectureRules
		.presentationTypesAreNotBeansOrEntities();

	@ArchTest
	static final ArchRule controllersAreNotPublic = ArchitectureRules.controllersAreNotPublic();

	@ArchTest
	static final ArchRule domainAndRepositoriesDoNotUseWebApis = ArchitectureRules
		.domainAndRepositoriesDoNotUseWebApis();

}
