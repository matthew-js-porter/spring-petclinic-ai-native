package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Exercises the visit reads and writes against the seeded H2. Each test rolls back, so
 * other tests still see the four seed visits.
 */
@DataJdbcTest
class VisitRepositoryTests {

	@Autowired
	private VisitRepository visits;

	@Autowired
	private JdbcClient jdbc;

	@Test
	void seedsFourVisitsForSamanthaAndMax() {
		assertThat(visitCount()).isEqualTo(4);
		assertThat(this.visits.findByPetIds(List.of(7, 8)))
			.extracting(Visit::id, Visit::petId, Visit::date, Visit::description)
			.containsExactlyInAnyOrder(tuple(1, 7, LocalDate.of(2013, 1, 1), "rabies shot"),
					tuple(2, 8, LocalDate.of(2013, 1, 2), "rabies shot"),
					tuple(3, 8, LocalDate.of(2013, 1, 3), "neutered"), tuple(4, 7, LocalDate.of(2013, 1, 4), "spayed"));
	}

	@Test
	void findsNoVisitsForAPetWithout() {
		assertThat(this.visits.findByPetIds(List.of(1))).isEmpty();
		assertThat(this.visits.findByPetIds(List.of(7))).extracting(Visit::id).containsExactlyInAnyOrder(1, 4);
	}

	@Test
	void insertsAVisitWithAnIdAboveTheSeed() {
		this.visits.insert(1, LocalDate.of(2024, 3, 5), "checkup");

		assertThat(this.visits.findByPetIds(List.of(1))).singleElement().satisfies((visit) -> {
			assertThat(visit.id()).isGreaterThan(4);
			assertThat(visit.petId()).isEqualTo(1);
			assertThat(visit.date()).isEqualTo(LocalDate.of(2024, 3, 5));
			assertThat(visit.description()).isEqualTo("checkup");
		});
		assertThat(visitCount()).isEqualTo(5);
	}

	@Test
	void rejectsAnUnknownPetAtTheForeignKey() {
		// Why the controller finds the pet before writing: the database answers with an
		// exception, which would otherwise be a 500.
		assertThatExceptionOfType(DataIntegrityViolationException.class)
			.isThrownBy(() -> this.visits.insert(999, LocalDate.of(2020, 1, 1), "ghost"));
	}

	private int visitCount() {
		return this.jdbc.sql("SELECT COUNT(*) FROM VISITS").query(Integer.class).single();
	}

}
