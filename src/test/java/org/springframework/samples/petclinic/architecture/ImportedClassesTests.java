package org.springframework.samples.petclinic.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the architecture rules against an empty import: ArchUnit skips class files its
 * bundled ASM cannot read (for example a newer Java class file version), and every rule
 * would then pass on nothing.
 */
class ImportedClassesTests {

	@Test
	void importsMainAndTestClasses() {
		JavaClasses classes = new ClassFileImporter().importPackages(ArchitectureRules.ROOT_PACKAGE);
		assertThat(classes.contain("org.springframework.samples.petclinic.owner.OwnerController")).isTrue();
		assertThat(classes.contain("org.springframework.samples.petclinic.vet.Vet")).isTrue();
		assertThat(classes.contain("org.springframework.samples.petclinic.owner.OwnerRepositoryTests")).isTrue();
	}

	@Test
	void importsEveryMainClass() {
		JavaClasses classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages(ArchitectureRules.ROOT_PACKAGE);
		assertThat(classes.stream().filter((javaClass) -> !javaClass.getSimpleName().equals("package-info")))
			.hasSizeGreaterThan(30);
	}

	@Test
	void everyLayerMatchesApplicationClasses() {
		JavaClasses classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
			.importPackages(ArchitectureRules.ROOT_PACKAGE);
		assertThat(classes.that(ArchitectureRules.WEB)).as("web types").isNotEmpty();
		assertThat(classes.that(ArchitectureRules.REPOSITORY)).as("repositories").isNotEmpty();
		assertThat(classes.that(ArchitectureRules.DOMAIN)).as("domain types").isNotEmpty();
		assertThat(classes.that(ArchitectureRules.PRESENTATION)).as("presentation types").isNotEmpty();
	}

}
