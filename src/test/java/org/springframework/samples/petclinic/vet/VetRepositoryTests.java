package org.springframework.samples.petclinic.vet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.samples.petclinic.StatementCounting;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs {@code schema.sql} and {@code data.sql} against embedded H2 and exercises the
 * {@link Vet} aggregate mapping and paging.
 */
@DataJdbcTest
@Import(StatementCounting.class)
class VetRepositoryTests {

	@Autowired
	private VetRepository vets;

	@Autowired
	private SpecialtyRepository specialties;

	@Test
	void seedsTheSixUpstreamVets() {
		List<Vet> all = this.vets.findAll(Sort.by("id"));

		assertThat(all).extracting((vet) -> vet.firstName() + " " + vet.lastName())
			.containsExactly("James Carter", "Helen Leary", "Linda Douglas", "Rafael Ortega", "Henry Stevens",
					"Sharon Jenkins");
	}

	@Test
	void seedsTheVetToSpecialtyLinks() {
		Map<Integer, List<Integer>> specialtyIdsByVet = this.vets.findAll(Sort.by("id"))
			.stream()
			.collect(Collectors.toMap(Vet::id,
					(vet) -> vet.specialties().stream().map((s) -> s.specialty().getId()).sorted().toList()));

		// 1 radiology, 2 surgery, 3 dentistry
		assertThat(specialtyIdsByVet).containsEntry(1, List.of())
			.containsEntry(2, List.of(1))
			.containsEntry(3, List.of(2, 3))
			.containsEntry(4, List.of(2))
			.containsEntry(5, List.of(1))
			.containsEntry(6, List.of());
	}

	@Test
	void seedsThreeSpecialties() {
		assertThat(this.specialties.findAll()).containsExactlyInAnyOrder(new Specialty(1, "radiology"),
				new Specialty(2, "surgery"), new Specialty(3, "dentistry"));
	}

	@Test
	void pagesVetsByIdWithSpecialtiesPopulated() {
		Page<Vet> first = this.vets.findAll(PageRequest.of(0, 5, Sort.by("id")));
		Page<Vet> second = this.vets.findAll(PageRequest.of(1, 5, Sort.by("id")));

		assertThat(first.getContent()).hasSize(5);
		assertThat(first.getTotalElements()).isEqualTo(6);
		assertThat(first.getContent()).filteredOn((vet) -> vet.id() == 3)
			.singleElement()
			.satisfies((vet) -> assertThat(vet.specialties()).hasSize(2));
		assertThat(second.getContent()).extracting(Vet::lastName).containsExactly("Jenkins");
	}

	@Test
	void readsAPageWithQueriesBoundedByThePageSize() {
		StatementCounting.reset();

		this.vets.findAll(PageRequest.of(0, 5, Sort.by("id")));

		// page query + count query + one specialties query per vet on the page
		assertThat(StatementCounting.count()).isLessThanOrEqualTo(2 + 5);
	}

}
