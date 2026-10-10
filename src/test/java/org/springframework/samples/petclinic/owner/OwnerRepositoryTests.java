package org.springframework.samples.petclinic.owner;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs {@code schema.sql} and {@code data.sql} against embedded H2 and exercises the
 * {@link Owner} aggregate mapping and paging.
 */
@DataJdbcTest
class OwnerRepositoryTests {

	private static final AtomicInteger statements = new AtomicInteger();

	@Autowired
	private OwnerRepository owners;

	@Autowired
	private JdbcClient jdbc;

	@Test
	void seedsTheTenUpstreamOwnersAndOneWithoutPets() {
		List<Owner> all = this.owners.findAll(Sort.by("id"));

		assertThat(all).extracting((owner) -> owner.firstName() + " " + owner.lastName())
			.containsExactly("George Franklin", "Betty Davis", "Eduardo Rodriquez", "Harold Davis", "Peter McTavish",
					"Jean Coleman", "Jeff Black", "Maria Escobito", "David Schroeder", "Carlos Estaban",
					"Helen Ostrander");
		assertThat(all.getFirst()).satisfies((george) -> {
			assertThat(george.address()).isEqualTo("110 W. Liberty St.");
			assertThat(george.city()).isEqualTo("Madison");
			assertThat(george.telephone()).isEqualTo("6085551023");
		});
	}

	@Test
	void seedsThirteenPetsAndSixTypes() {
		assertThat(this.jdbc.sql("SELECT COUNT(*) FROM pets").query(Integer.class).single()).isEqualTo(13);
		assertThat(this.jdbc.sql("SELECT name FROM types ORDER BY id").query(String.class).list())
			.containsExactly("cat", "dog", "lizard", "snake", "bird", "hamster");
	}

	@Test
	void seedsThePetsOfEachOwner() {
		Map<Integer, List<String>> petsByOwner = this.owners.findAll(Sort.by("id"))
			.stream()
			.collect(Collectors.toMap(Owner::id,
					(owner) -> owner.pets().stream().map(Pet::name).sorted().toList()));

		assertThat(petsByOwner).containsEntry(1, List.of("Leo"))
			.containsEntry(2, List.of("Basil"))
			.containsEntry(3, List.of("Jewel", "Rosy"))
			.containsEntry(4, List.of("Iggy"))
			.containsEntry(5, List.of("George"))
			.containsEntry(6, List.of("Max", "Samantha"))
			.containsEntry(7, List.of("Lucky"))
			.containsEntry(8, List.of("Mulligan"))
			.containsEntry(9, List.of("Freddy"))
			.containsEntry(10, List.of("Lucky", "Sly"))
			.containsEntry(11, List.of());
	}

	@Test
	void mapsPetBirthDateAndType() {
		Pet leo = this.owners.findAll(Sort.by("id")).getFirst().pets().iterator().next();

		assertThat(leo.birthDate()).hasToString("2010-09-07");
		assertThat(leo.type().getId()).isEqualTo(1);
	}

	@Test
	void pagesOwnersByIdWithPetsPopulated() {
		Page<Owner> first = this.owners.findAll(PageRequest.of(0, 5, Sort.by("id")));
		Page<Owner> second = this.owners.findAll(PageRequest.of(1, 5, Sort.by("id")));
		Page<Owner> third = this.owners.findAll(PageRequest.of(2, 5, Sort.by("id")));

		assertThat(first.getContent()).extracting(Owner::id).containsExactly(1, 2, 3, 4, 5);
		assertThat(first.getTotalElements()).isEqualTo(11);
		assertThat(first.getTotalPages()).isEqualTo(3);
		assertThat(first.getContent()).allSatisfy((owner) -> assertThat(owner.pets()).isNotEmpty());
		assertThat(second.getContent()).extracting(Owner::id).containsExactly(6, 7, 8, 9, 10);
		assertThat(third.getContent()).extracting(Owner::id).containsExactly(11);
	}

	@Test
	void findsAnOwnerByIdWithTheirPets() {
		assertThat(this.owners.findById(1)).hasValueSatisfying((george) -> {
			assertThat(george.firstName() + " " + george.lastName()).isEqualTo("George Franklin");
			assertThat(george.pets()).extracting(Pet::name).containsExactly("Leo");
		});
		assertThat(this.owners.findById(6)).hasValueSatisfying(
				(jean) -> assertThat(jean.pets()).extracting(Pet::name).containsExactlyInAnyOrder("Max", "Samantha"));
	}

	@Test
	void findsTheOwnerWithoutPets() {
		assertThat(this.owners.findById(11)).hasValueSatisfying((helen) -> {
			assertThat(helen.firstName() + " " + helen.lastName()).isEqualTo("Helen Ostrander");
			assertThat(helen.address()).isEqualTo("4120 Monroe St.");
			assertThat(helen.city()).isEqualTo("Fitchburg");
			assertThat(helen.telephone()).isEqualTo("6085554412");
			assertThat(helen.pets()).isEmpty();
		});
	}

	@ParameterizedTest
	@ValueSource(ints = { 999, 0, -1 })
	void findsNoOwnerForAnUnknownId(int id) {
		assertThat(this.owners.findById(id)).isEmpty();
	}

	@Test
	void readsAPageWithQueriesBoundedByThePageSize() {
		statements.set(0);

		this.owners.findAll(PageRequest.of(0, 5, Sort.by("id")));

		// page query + count query + one pets query per owner on the page
		assertThat(statements.get()).isLessThanOrEqualTo(2 + OwnerController.PAGE_SIZE);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Davis", "Dav", "davis", "DAVIS", "D" })
	void findsOwnersByLastNamePrefixIgnoringCase(String term) {
		assertThat(this.owners.findByLastNameStartingWithIgnoreCase(term, Sort.by("id"))).extracting(Owner::id)
			.containsExactly(2, 4);
	}

	@Test
	void findsEveryOwnerWithTheSameInitial() {
		assertThat(this.owners.findByLastNameStartingWithIgnoreCase("E", Sort.by("id")))
			.extracting(Owner::lastName)
			.containsExactly("Escobito", "Estaban");
	}

	@ParameterizedTest
	@ValueSource(strings = { "Dx", "%", "_", "\\", "D%s", "_avis" })
	void findsNoOwnerWhenNothingMatchesOrTheTermHoldsWildcards(String term) {
		assertThat(this.owners.findByLastNameStartingWithIgnoreCase(term, Sort.by("id"))).isEmpty();
	}

	@Test
	void pagesFilteredOwnersWithPetsPopulated() {
		Page<Owner> page = this.owners.findByLastNameStartingWithIgnoreCase("e",
				PageRequest.of(0, OwnerController.PAGE_SIZE, Sort.by("id")));

		assertThat(page.getContent()).extracting(Owner::id).containsExactly(8, 10);
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getTotalPages()).isEqualTo(1);
		assertThat(page.getContent()).allSatisfy((owner) -> assertThat(owner.pets()).isNotEmpty());
	}

	@Test
	void readsAFilteredPageWithQueriesBoundedByThePageSize() {
		statements.set(0);

		this.owners.findByLastNameStartingWithIgnoreCase("D",
				PageRequest.of(0, OwnerController.PAGE_SIZE, Sort.by("id")));

		assertThat(statements.get()).isLessThanOrEqualTo(2 + OwnerController.PAGE_SIZE);
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
