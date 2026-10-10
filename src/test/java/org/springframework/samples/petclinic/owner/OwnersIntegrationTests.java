package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check of {@code GET /owners} with the real database. The rendered page is
 * covered by {@link OwnersPageHtmlUnitTests}.
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
			.hasSize(10);
		assertThat(this.mvc.get().uri("/owners")).bodyJson()
			.extractingPath("$.ownerList[*].pets[*]")
			.asArray()
			.hasSize(13);
	}

}
