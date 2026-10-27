package org.springframework.samples.petclinic.owner;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check of {@code GET /owners} and {@code GET /owners/{id}} with the real
 * database. The rendered pages are covered by {@link OwnersPageHtmlUnitTests} and
 * {@link OwnerDetailsPageHtmlUnitTests}. Tests that write roll back, so the seed count
 * holds for every other test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OwnersIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void returnsTheSeededOwnersAndPetsAsJson() {
		assertThat(this.mvc.get().uri("/owners").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.ownerList")
			.asArray()
			.hasSize(11);
		assertThat(this.mvc.get().uri("/owners")).bodyJson()
			.extractingPath("$.ownerList[*].pets[*]")
			.asArray()
			.hasSize(13);
	}

	@ParameterizedTest
	@CsvSource({ "Davis, 2", "dav, 2", "e, 2", "zzz, 0", "%, 0" })
	void filtersTheJsonByLastNamePrefix(String term, int owners) {
		assertThat(this.mvc.get().uri("/owners?lastName={term}", term).accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.ownerList")
			.asArray()
			.hasSize(owners);
	}

	@Test
	void returnsOneOwnerWithPetBirthDatesTypesAndVisitsAsJson() {
		assertThat(this.mvc.get().uri("/owners/6").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.isStrictlyEqualTo("""
					{"id":6,"firstName":"Jean","lastName":"Coleman","address":"105 N. Lake St.","city":"Monona",
					 "telephone":"6085552654","pets":[
					  {"id":8,"name":"Max","birthDate":"2012-09-04","type":"cat","visits":[
					   {"id":3,"date":"2013-01-03","description":"neutered"},
					   {"id":2,"date":"2013-01-02","description":"rabies shot"}]},
					  {"id":7,"name":"Samantha","birthDate":"2012-09-04","type":"cat","visits":[
					   {"id":4,"date":"2013-01-04","description":"spayed"},
					   {"id":1,"date":"2013-01-01","description":"rabies shot"}]}]}
					""");
		assertThat(this.mvc.get().uri("/owners/3")).bodyJson()
			.extractingPath("$.pets[*].visits")
			.asArray()
			.containsExactly(List.of(), List.of());
		assertThat(this.mvc.get().uri("/owners")).bodyJson()
			.extractingPath("$.ownerList[*].pets[*].visits")
			.asArray()
			.isEmpty();
		assertThat(this.mvc.get().uri("/owners/11")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"id":11,"firstName":"Helen","lastName":"Ostrander","pets":[]}
				""");
	}

	@Test
	@Transactional
	void showsACreatedOwnerInTheJson() {
		MvcTestResult created = this.mvc.post()
			.uri("/owners")
			.param("firstName", "Ada")
			.param("lastName", "Lovelace")
			.param("address", "12 St James's Sq.")
			.param("city", "London")
			.param("telephone", "6085550000")
			.exchange();
		assertThat(created).hasStatus(HttpStatus.SEE_OTHER);
		String location = created.getResponse().getRedirectedUrl();
		assertThat(location).isNotNull();
		assertThat(location).matches("/owners/\\d+\\.html");
		String json = location.replace(".html", "");

		assertThat(this.mvc.get().uri("/owners")).bodyJson().extractingPath("$.ownerList").asArray().hasSize(12);
		assertThat(this.mvc.get().uri(json)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
				{"firstName":"Ada","lastName":"Lovelace","city":"London","telephone":"6085550000","pets":[]}
				""");
	}

	@Test
	@Transactional
	void showsAddedAndEditedPetsInTheJson() {
		assertThat(this.mvc.post()
			.uri("/owners/11/pets")
			.param("name", "Biscuit")
			.param("birthDate", "2024-05-01")
			.param("typeId", "6")).hasStatus(HttpStatus.SEE_OTHER).hasRedirectedUrl("/owners/11.html");
		assertThat(this.mvc.post()
			.uri("/owners/6/pets/8")
			.param("name", "Maximus")
			.param("birthDate", "2012-09-05")
			.param("typeId", "2")).hasStatus(HttpStatus.SEE_OTHER).hasRedirectedUrl("/owners/6.html");

		assertThat(this.mvc.get().uri("/owners/11")).bodyJson().isLenientlyEqualTo("""
				{"id":11,"pets":[{"name":"Biscuit","birthDate":"2024-05-01","type":"hamster"}]}
				""");
		assertThat(this.mvc.get().uri("/owners/11")).bodyJson()
			.extractingPath("$.pets[0].id")
			.asNumber()
			.satisfies((id) -> assertThat(id.intValue()).isGreaterThan(13));
		assertThat(this.mvc.get().uri("/owners/6")).bodyJson().isLenientlyEqualTo("""
				{"id":6,"pets":[
				  {"id":8,"name":"Maximus","birthDate":"2012-09-05","type":"dog"},
				  {"id":7,"name":"Samantha","birthDate":"2012-09-04","type":"cat"}]}
				""");
		assertThat(this.mvc.get().uri("/owners")).bodyJson()
			.extractingPath("$.ownerList[*].pets[*]")
			.asArray()
			.hasSize(14);
		assertThat(this.mvc.get().uri("/owners?lastName=Ostrander")).bodyJson()
			.extractingPath("$.ownerList[0].pets[*].name")
			.asArray()
			.containsExactly("Biscuit");
	}

	@Test
	@Transactional
	void showsAnAddedVisitInTheJson() {
		assertThat(this.mvc.post()
			.uri("/owners/6/pets/8/visits")
			.param("date", "2013-01-03")
			.param("description", "<script>alert(1)</script> & \"x\" 'y'")).hasStatus(HttpStatus.SEE_OTHER)
			.hasRedirectedUrl("/owners/6.html");

		assertThat(this.mvc.get().uri("/owners/6")).bodyJson()
			.extractingPath("$.pets[0].visits[*].description")
			.asArray()
			.containsExactly("<script>alert(1)</script> & \"x\" 'y'", "neutered", "rabies shot");
		assertThat(this.mvc.get().uri("/owners/6")).bodyJson()
			.extractingPath("$.pets[0].visits[0].id")
			.asNumber()
			.satisfies((id) -> assertThat(id.intValue()).isGreaterThan(4));
	}

	@Test
	void returnsNotFoundForAnUnknownOwner() {
		assertThat(this.mvc.get().uri("/owners/999")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(this.mvc.get().uri("/owners/999.html")).hasStatus(HttpStatus.NOT_FOUND);
	}

}
