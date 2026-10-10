package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check of {@code GET /owners} and {@code GET /owners/{id}} with the real
 * database. The rendered pages are covered by {@link OwnersPageHtmlUnitTests} and
 * {@link OwnerDetailsPageHtmlUnitTests}.
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
		assertThat(this.mvc.get().uri("/owners?lastName={term}", term).accept(MediaType.APPLICATION_JSON))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.ownerList")
			.asArray()
			.hasSize(owners);
	}

	@Test
	void returnsOneOwnerWithPetBirthDatesAndTypesAsJson() {
		assertThat(this.mvc.get().uri("/owners/6").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"id":6,"firstName":"Jean","lastName":"Coleman","address":"105 N. Lake St.","city":"Monona",
					 "telephone":"6085552654","pets":[
					  {"id":8,"name":"Max","birthDate":"2012-09-04","type":"cat"},
					  {"id":7,"name":"Samantha","birthDate":"2012-09-04","type":"cat"}]}
					""");
		assertThat(this.mvc.get().uri("/owners/11")).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"id":11,"firstName":"Helen","lastName":"Ostrander","pets":[]}
					""");
	}

	@Test
	void returnsNotFoundForAnUnknownOwner() {
		assertThat(this.mvc.get().uri("/owners/999")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(this.mvc.get().uri("/owners/999.html")).hasStatus(HttpStatus.NOT_FOUND);
	}

}
