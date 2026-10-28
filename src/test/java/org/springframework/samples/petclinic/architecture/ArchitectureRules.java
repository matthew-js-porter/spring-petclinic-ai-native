package org.springframework.samples.petclinic.architecture;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.Repository;
import org.springframework.samples.petclinic.PetClinicApplication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.conditions.ArchConditions.be;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * The architecture rules, shared by {@link HygieneRulesTests}, {@link LayeringRulesTests},
 * {@link TestConventionsRulesTests} and {@link ArchitectureRulesTests}. Rules that depend on package names take the
 * root package so the self tests can run them against fixtures.
 * <p>
 * The feature packages are flat, so layers are identified by role: web types by their
 * stereotype, repositories by their supertype, domain types by {@code @Table}, and
 * presentation types by name.
 */
final class ArchitectureRules {

	static final String ROOT_PACKAGE = "org.springframework.samples.petclinic";

	/** Presentation types carry no stereotype, so they are matched by name. */
	private static final List<String> PRESENTATION_SUFFIXES = List.of("View", "Form");

	private static final List<String> PRESENTATION_NAMES = List.of("Owners", "Vets");

	static final DescribedPredicate<JavaClass> WEB = DescribedPredicate.describe("web types",
			(javaClass) -> javaClass.isAnnotatedWith(Controller.class)
					|| javaClass.isAnnotatedWith(RestController.class)
					|| javaClass.isAnnotatedWith(ControllerAdvice.class));

	static final DescribedPredicate<JavaClass> REPOSITORY = DescribedPredicate.describe("repositories",
			(javaClass) -> javaClass.isInterface() && javaClass.isAssignableTo(Repository.class));

	static final DescribedPredicate<JavaClass> DOMAIN = DescribedPredicate.describe("domain types",
			(javaClass) -> javaClass.isAnnotatedWith(Table.class));

	static final DescribedPredicate<JavaClass> PRESENTATION = DescribedPredicate.describe("presentation types",
			(javaClass) -> PRESENTATION_NAMES.contains(javaClass.getSimpleName())
					|| PRESENTATION_SUFFIXES.stream().anyMatch(javaClass.getSimpleName()::endsWith));

	/**
	 * A call to {@code printStackTrace()} on any {@code Throwable}: the compiler records the
	 * receiver's declared type as the owner, so matching {@code Throwable} alone misses
	 * {@code exception.printStackTrace()}.
	 */
	private static final DescribedPredicate<JavaMethodCall> PRINT_STACK_TRACE = DescribedPredicate.describe(
			"Throwable.printStackTrace()",
			(call) -> call.getName().equals("printStackTrace") && call.getTarget().getRawParameterTypes().isEmpty()
					&& call.getTargetOwner().isAssignableTo(Throwable.class));

	/** A class that declares at least one Jupiter test method. */
	static final DescribedPredicate<JavaClass> TEST_CLASS = DescribedPredicate.describe("test classes",
			(javaClass) -> javaClass.getMethods()
				.stream()
				.anyMatch((method) -> method.isAnnotatedWith(Test.class)
						|| method.isAnnotatedWith(ParameterizedTest.class)));

	private ArchitectureRules() {
	}

	// Hygiene rules: main and test code

	static ArchRule packagesAreFreeOfCycles() {
		return slices().matching("(**)")
			.should()
			.beFreeOfCycles()
			.as("Packages are free of cycles")
			.because("a cycle couples features; move the shared type into the package that owns it");
	}

	static ArchRule caseConversionUsesLocale() {
		return noClasses().should()
			.callMethod(String.class, "toUpperCase")
			.orShould()
			.callMethod(String.class, "toLowerCase")
			.as("No String.toUpperCase() or toLowerCase() without a Locale")
			.because("the result depends on the default locale; use toUpperCase(Locale.ROOT) or the user's locale");
	}

	static ArchRule streamsUseToList() {
		return noClasses().should()
			.callMethod(Collectors.class, "toList")
			.as("No Collectors.toList()")
			.because("Stream.toList() is shorter and returns an unmodifiable list; use stream.toList()");
	}

	static ArchRule urlCodingUsesCharset() {
		return noClasses().should()
			.callMethod(URLEncoder.class, "encode", String.class)
			.orShould()
			.callMethod(URLEncoder.class, "encode", String.class, String.class)
			.orShould()
			.callMethod(URLDecoder.class, "decode", String.class)
			.orShould()
			.callMethod(URLDecoder.class, "decode", String.class, String.class)
			.as("No URLEncoder.encode or URLDecoder.decode without a Charset")
			.because("the one-argument forms use the platform charset and the String forms take an unchecked "
					+ "charset name; pass StandardCharsets.UTF_8");
	}

	/**
	 * Spring Boot's two post-processor rules in one, so the rule always has
	 * {@code BeanPostProcessor} methods to check and needs no
	 * {@code allowEmptyShould(true)}.
	 */
	static ArchRule postProcessorBeanMethodsAreStaticAndDoNotInitializeEarly() {
		return methods().that()
			.areAnnotatedWith(Bean.class)
			.and()
			.haveRawReturnType(assignableTo(BeanPostProcessor.class).or(assignableTo(BeanFactoryPostProcessor.class)))
			.should()
			.beStatic()
			.andShould(notHaveParametersThatInitializeEarly())
			.as("@Bean methods returning a BeanPostProcessor or BeanFactoryPostProcessor are static and take no "
					+ "parameter that is initialized early")
			.because("a non-static method creates its configuration class too early, and a parameter is created "
					+ "before it can be post-processed; add static, inject a BeanPostProcessor's collaborators as "
					+ "ObjectProvider or @Lazy, and give a BeanFactoryPostProcessor only the Environment");
	}

	static ArchRule beanMethodsDoNotReturnPrivateTypes() {
		return methods().that()
			.areAnnotatedWith(Bean.class)
			.should()
			.haveRawReturnType(DescribedPredicate.not(modifier(JavaModifier.PRIVATE)).as("a non-private type"))
			.as("@Bean methods do not return private types")
			.because("Spring AOT processing cannot reference a private type; return a package-private or public type "
					+ "or an interface");
	}

	static ArchRule noStandardStreams() {
		return noClasses().should()
			.accessField(System.class, "out")
			.orShould()
			.accessField(System.class, "err")
			.orShould()
			.callMethodWhere(PRINT_STACK_TRACE)
			.as("No System.out, System.err or printStackTrace()")
			.because("output bypasses logging configuration; use an SLF4J logger");
	}

	static ArchRule packagesAreNullMarked() {
		return classes().should(beInNullMarkedPackage())
			.as("Every package is @NullMarked")
			.because("JSpecify treats unannotated packages as unspecified; add package-info.java with @NullMarked");
	}

	// Test conventions: test code only, from the Spring Framework and Boot test rules

	static ArchRule testMethodsAreNotPublicOrPrivate() {
		return methods().that()
			.areAnnotatedWith(Test.class)
			.or()
			.areAnnotatedWith(ParameterizedTest.class)
			.should()
			.notBePublic()
			.andShould()
			.notBePrivate()
			.as("Test methods are package-private")
			.because("JUnit Jupiter does not need public methods and private ones are not run; remove the modifier");
	}

	static ArchRule testClassesAreNotPublic() {
		return classes().that(TEST_CLASS)
			.should()
			.notBePublic()
			.as("Test classes are package-private")
			.because("JUnit Jupiter does not need public classes; remove the modifier");
	}

	static ArchRule testClassesAreNamedTests() {
		return classes().that(TEST_CLASS)
			.and()
			.areTopLevelClasses()
			.should()
			.haveSimpleNameEndingWith("Tests")
			.as("Test classes are named *Tests")
			.because("Surefire and the other test classes use the Tests suffix; rename the class");
	}

	// Layering rules: main code only

	static ArchRule layersAreRespected() {
		return layeredArchitecture().consideringOnlyDependenciesInLayers()
			.layer("Web")
			.definedBy(WEB)
			.layer("Repository")
			.definedBy(REPOSITORY)
			.layer("Domain")
			.definedBy(DOMAIN)
			.layer("Presentation")
			.definedBy(PRESENTATION)
			.whereLayer("Web")
			.mayNotBeAccessedByAnyLayer()
			.whereLayer("Repository")
			.mayOnlyBeAccessedByLayers("Web")
			.whereLayer("Presentation")
			.mayOnlyBeAccessedByLayers("Web")
			.whereLayer("Domain")
			.mayOnlyBeAccessedByLayers("Web", "Repository", "Presentation")
			.as("Web uses repositories, domain and presentation types; repositories use the domain; presentation types use the domain")
			.because("lower layers must not reach up; move the logic into the controller or pass the data in");
	}

	static ArchRule domainAndRepositoriesDoNotDependOnWeb() {
		return noClasses().that(DOMAIN.or(REPOSITORY))
			.should()
			.dependOnClassesThat(WEB)
			.as("Domain types and repositories do not depend on web types")
			.because("the web layer depends on them, not the reverse; move the code into the controller");
	}

	static ArchRule featuresDoNotDependOnEachOther(String rootPackage) {
		return slices().matching(rootPackage + ".(*)..")
			.should()
			.notDependOnEachOther()
			.as("Feature packages under " + rootPackage + " do not depend on each other")
			.because("features change independently; duplicate the small type or pass plain values");
	}

	static ArchRule featuresDoNotDependOnRootPackage(String rootPackage) {
		return noClasses().that()
			.resideInAPackage(rootPackage + ".*..")
			.should()
			.dependOnClassesThat()
			.resideInAPackage(rootPackage)
			.as("Feature packages do not depend on the root package " + rootPackage)
			.because("the root package wires the features together; move the shared type into the feature that owns it");
	}

	static ArchRule applicationClassIsNotADependency() {
		return noClasses().that()
			.doNotBelongToAnyOf(PetClinicApplication.class)
			.should()
			.dependOnClassesThat()
			.belongToAnyOf(PetClinicApplication.class)
			.as("No class depends on PetClinicApplication")
			.because("it only boots the application; declare shared beans in the feature that uses them");
	}

	static ArchRule noFieldInjection() {
		return noFields().should()
			.beAnnotatedWith(Autowired.class)
			.orShould()
			.beAnnotatedWith("jakarta.inject.Inject")
			.orShould()
			.beAnnotatedWith("jakarta.annotation.Resource")
			.as("No field injection")
			.because("injected fields hide dependencies and cannot be final; take collaborators through the constructor");
	}

	static ArchRule controllersLiveInRootOrFeaturePackage(String rootPackage) {
		return classes().that(WEB)
			.should()
			.resideInAnyPackage(rootPackage, rootPackage + ".*")
			.as("Web types reside in the root package or a feature package")
			.because("a feature's controllers sit beside its types; do not create web sub-packages");
	}

	static ArchRule controllersAreNamedController() {
		return classes().that()
			.areAnnotatedWith(Controller.class)
			.or()
			.areAnnotatedWith(RestController.class)
			.should()
			.haveSimpleNameEndingWith("Controller")
			.as("Controllers are named *Controller")
			.because("the suffix is how readers and these rules recognise the web layer; rename the class");
	}

	static ArchRule controllerNamedTypesAreControllers() {
		return classes().that()
			.haveSimpleNameEndingWith("Controller")
			.should()
			.beAnnotatedWith(Controller.class)
			.orShould()
			.beAnnotatedWith(RestController.class)
			.as("*Controller types are annotated @Controller or @RestController")
			.because("the name promises a web stereotype; annotate the class or rename it");
	}

	static ArchRule repositoriesAreNamedRepository() {
		return classes().that(REPOSITORY)
			.should()
			.haveSimpleNameEndingWith("Repository")
			.as("Repositories are named *Repository")
			.because("the suffix is how readers and these rules recognise the repository layer; rename the interface");
	}

	static ArchRule repositoryNamedTypesAreRepositories() {
		return classes().that()
			.haveSimpleNameEndingWith("Repository")
			.should()
			.beInterfaces()
			.andShould()
			.beAssignableTo(Repository.class)
			.as("*Repository types are interfaces extending Spring Data's Repository")
			.because("Spring Data implements repositories; declare an interface extending Repository<T, ID>");
	}

	static ArchRule domainTypesAreNotOtherRoles() {
		return noClasses().that(DOMAIN)
			.should(be(PRESENTATION.or(WEB)))
			.orShould()
			.haveSimpleNameEndingWith("Controller")
			.orShould()
			.haveSimpleNameEndingWith("Repository")
			.as("@Table types are named for the entity and carry no web stereotype")
			.because("an entity is not a view, form, controller or repository; split the type");
	}

	static ArchRule presentationTypesAreNotBeansOrEntities() {
		return noClasses().that(PRESENTATION)
			.should()
			.beMetaAnnotatedWith(Component.class)
			.orShould()
			.beAnnotatedWith(Table.class)
			.as("*View and *Form types carry no Spring stereotype and no @Table")
			.because("they are plain records built per request; remove the annotation");
	}

	static ArchRule controllersAreNotPublic() {
		return classes().that(WEB)
			.should()
			.notBePublic()
			.as("Web types are not public")
			.because("no other package should call a controller; make the class package-private");
	}

	static ArchRule domainAndRepositoriesDoNotUseWebApis() {
		return noClasses().that(DOMAIN.or(REPOSITORY))
			.should()
			.dependOnClassesThat()
			.resideInAnyPackage("jakarta.servlet..", "org.springframework.web..")
			.as("Domain types and repositories do not use servlet or Spring web APIs")
			.because("request handling belongs to the web layer; do the conversion in the controller");
	}

	/**
	 * As in Spring Boot's rules: a {@code BeanFactoryPostProcessor} may inject only the
	 * {@link Environment}; a {@code BeanPostProcessor} may also inject {@code @Lazy}
	 * parameters, an {@link ObjectProvider}, the {@link ApplicationContext} and
	 * infrastructure beans.
	 */
	private static ArchCondition<JavaMethod> notHaveParametersThatInitializeEarly() {
		return new ArchCondition<>("not have parameters that are initialized early") {

			@Override
			public void check(JavaMethod method, ConditionEvents events) {
				boolean factoryPostProcessor = method.getRawReturnType().isAssignableTo(BeanFactoryPostProcessor.class);
				for (JavaParameter parameter : method.getParameters()) {
					if (factoryPostProcessor && !parameter.getRawType().isEquivalentTo(Environment.class)) {
						events.add(SimpleConditionEvent.violated(parameter, parameter.getDescription()
								+ " is not the Environment; look it up from the bean factory instead"));
					}
					else if (!factoryPostProcessor && !parameter.isAnnotatedWith(Lazy.class)
							&& !isSafeToInjectEarly(parameter.getRawType())) {
						events.add(SimpleConditionEvent.violated(parameter, parameter.getDescription()
								+ " is initialized before it can be post-processed; inject an ObjectProvider or add @Lazy"));
					}
				}
			}

		};
	}

	private static boolean isSafeToInjectEarly(JavaClass type) {
		return type.isAssignableTo(ObjectProvider.class) || type.isAssignableTo(ApplicationContext.class)
				|| type.isAssignableTo(Environment.class) || isInfrastructure(type);
	}

	private static boolean isInfrastructure(JavaClass type) {
		return type.tryGetAnnotationOfType(Role.class)
			.map((role) -> role.value() == BeanDefinition.ROLE_INFRASTRUCTURE)
			.orElse(false);
	}

	private static ArchCondition<JavaClass> beInNullMarkedPackage() {
		return new ArchCondition<>("be in a package annotated with @NullMarked") {

			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				JavaPackage javaPackage = javaClass.getPackage();
				if (!javaPackage.isAnnotatedWith(NullMarked.class)) {
					events.add(SimpleConditionEvent.violated(javaClass, javaClass.getName() + " is in package "
							+ javaPackage.getName() + ", which has no package-info.java annotated with @NullMarked"));
				}
			}

		};
	}

}
