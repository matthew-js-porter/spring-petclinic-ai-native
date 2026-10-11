package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Exercises the pet writes against the seeded H2. Each test rolls back, so other tests
 * still see the thirteen seed pets.
 */
@DataJdbcTest
class PetRepositoryTests {

	@Autowired
	private PetRepository pets;

	@Autowired
	private OwnerRepository owners;

	@Autowired
	private JdbcClient jdbc;

	@Test
	void insertsAPetForAnOwnerWithAnIdAboveTheSeed() {
		this.pets.insert(11, "Rex", LocalDate.of(2023, 4, 12), 2);

		assertThat(this.owners.findById(11)).hasValueSatisfying((helen) -> assertThat(helen.pets()).singleElement()
			.satisfies((rex) -> {
				assertThat(rex.id()).isGreaterThan(13);
				assertThat(rex.name()).isEqualTo("Rex");
				assertThat(rex.birthDate()).isEqualTo(LocalDate.of(2023, 4, 12));
				assertThat(rex.type().getId()).isEqualTo(2);
			}));
		assertThat(petCount()).isEqualTo(14);
	}

	@Test
	void updatesOnlyTheNameBirthDateAndTypeOfOnePet() {
		assertThat(this.pets.update(1, 1, "Leonard", LocalDate.of(2011, 1, 2), 2)).isTrue();

		assertThat(this.owners.findById(1)).hasValueSatisfying((george) -> assertThat(george.pets()).singleElement()
			.satisfies((pet) -> {
				assertThat(pet.id()).isEqualTo(1);
				assertThat(pet.name()).isEqualTo("Leonard");
				assertThat(pet.birthDate()).isEqualTo(LocalDate.of(2011, 1, 2));
				assertThat(pet.type().getId()).isEqualTo(2);
			}));
		assertThat(this.owners.findById(1)).hasValueSatisfying((george) -> assertThat(george.lastName()).isEqualTo("Franklin"));
		assertThat(petCount()).isEqualTo(13);
	}

	@Test
	void keepsTheOtherPetsAndTheirIdsWhenUpdatingOne() {
		this.pets.update(6, 8, "Maxwell", LocalDate.of(2012, 9, 4), 1);

		assertThat(this.owners.findById(6)).hasValueSatisfying((jean) -> assertThat(jean.pets())
			.extracting(Pet::id, Pet::name)
			.containsExactlyInAnyOrder(tuple(7, "Samantha"), tuple(8, "Maxwell")));
	}

	@Test
	void updatesNothingThroughAnotherOwner() {
		assertThat(this.pets.update(1, 2, "Stolen", LocalDate.of(2012, 8, 6), 6)).isFalse();

		assertThat(this.owners.findById(2)).hasValueSatisfying((betty) -> assertThat(betty.pets())
			.extracting(Pet::id, Pet::name)
			.containsExactly(tuple(2, "Basil")));
		assertThat(this.owners.findById(1)).hasValueSatisfying(
				(george) -> assertThat(george.pets()).extracting(Pet::name).containsExactly("Leo"));
	}

	@Test
	void rejectsAnUnknownTypeAtTheForeignKey() {
		// Why the controller checks the type before writing: the database answers with an
		// exception, which would otherwise be a 500.
		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> this.pets.insert(1, "Ghost", LocalDate.of(2020, 1, 1), 7));
	}

	private int petCount() {
		return this.jdbc.sql("SELECT COUNT(*) FROM PETS").query(Integer.class).single();
	}

}
