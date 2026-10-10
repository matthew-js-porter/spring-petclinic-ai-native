package org.springframework.samples.petclinic.vet;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs {@code schema.sql} and {@code data.sql} against embedded H2 and exercises the
 * {@link Vet} aggregate mapping and paging.
 */
@DataJdbcTest
class VetRepositoryTests {

	private static final AtomicInteger statements = new AtomicInteger();

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
		Map<Integer, String> names = this.specialties.findAll()
			.stream()
			.collect(Collectors.toMap(Specialty::id, Specialty::name));
		Map<Integer, List<String>> byVet = this.vets.findAll(Sort.by("id"))
			.stream()
			.collect(Collectors.toMap(Vet::id,
					(vet) -> vet.specialties().stream().map((s) -> names.get(s.specialty().getId())).sorted().toList()));

		assertThat(byVet).containsEntry(1, List.of())
			.containsEntry(2, List.of("radiology"))
			.containsEntry(3, List.of("dentistry", "surgery"))
			.containsEntry(4, List.of("surgery"))
			.containsEntry(5, List.of("radiology"))
			.containsEntry(6, List.of());
	}

	@Test
	void seedsThreeSpecialties() {
		assertThat(this.specialties.findAll()).extracting(Specialty::name)
			.containsExactlyInAnyOrder("radiology", "surgery", "dentistry");
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
		statements.set(0);

		this.vets.findAll(PageRequest.of(0, 5, Sort.by("id")));

		// page query + count query + one specialties query per vet on the page
		assertThat(statements.get()).isLessThanOrEqualTo(2 + 5);
	}

	@TestConfiguration
	static class StatementCounting {

		@Bean
		static BeanPostProcessor countingDataSource() {
			return new BeanPostProcessor() {
				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) {
					return (bean instanceof DataSource dataSource) ? counting(dataSource) : bean;
				}
			};
		}

		private static DataSource counting(DataSource target) {
			return (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
					new Class<?>[] { DataSource.class }, (proxy, method, args) -> {
						Object result = method.invoke(target, args);
						return (result instanceof Connection connection) ? counting(connection) : result;
					});
		}

		private static Connection counting(Connection target) {
			return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
					new Class<?>[] { Connection.class }, (proxy, method, args) -> {
						if (method.getName().startsWith("prepare") || method.getName().equals("createStatement")) {
							statements.incrementAndGet();
						}
						return method.invoke(target, args);
					});
		}

	}

}
