package org.springframework.samples.petclinic.vet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end checks with the real database and actuator. The rendered page and its
 * stylesheets and scripts are covered by {@link VetsPageHtmlUnitTests}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VetsIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void returnsTheSeededVetsAsJson() {
		assertThat(this.mvc.get().uri("/vets").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.vetList")
			.asArray()
			.hasSize(6);
	}

	@ParameterizedTest
	@ValueSource(strings = { "/resources/fonts/montserrat-latin.woff2", "/resources/fonts/varela-round-latin.woff2" })
	void servesFonts(String path) {
		assertThat(this.mvc.get().uri(path)).hasStatusOk();
	}

	@Test
	void healthIsUpWithoutDetails() {
		assertThat(this.mvc.get().uri("/actuator/health")).hasStatusOk()
			.bodyJson()
			.doesNotHavePath("$.components")
			.doesNotHavePath("$.details")
			.extractingPath("$.status")
			.isEqualTo("UP");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/actuator/info", "/actuator/env", "/h2-console" })
	void exposesNothingElse(String path) {
		assertThat(this.mvc.get().uri(path)).hasStatus(HttpStatus.NOT_FOUND);
	}

}
