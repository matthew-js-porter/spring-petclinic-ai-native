package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Reads the seeded pet types through {@link PetTypeRepository}.
 */
@DataJdbcTest
class PetTypeRepositoryTests {

	@Autowired
	private PetTypeRepository petTypes;

	@Test
	void readsTheSixSeededTypes() {
		assertThat(this.petTypes.findAll()).extracting(PetType::id, PetType::name)
			.containsExactlyInAnyOrder(tuple(1, "cat"), tuple(2, "dog"), tuple(3, "lizard"), tuple(4, "snake"),
					tuple(5, "bird"), tuple(6, "hamster"));
	}

	@Test
	void findsOneTypeById() {
		assertThat(this.petTypes.findById(4)).map(PetType::name).contains("snake");
		assertThat(this.petTypes.findById(99)).isEmpty();
	}

}
