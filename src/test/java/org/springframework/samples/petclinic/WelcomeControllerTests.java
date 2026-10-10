package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(WelcomeController.class)
class WelcomeControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void showsWelcomePageAtRoot() {
		MvcTestResult result = this.mvc.get().uri("/").exchange();
		assertThat(result).hasStatusOk().hasContentTypeCompatibleWith(MediaType.TEXT_HTML).hasViewName("welcome");
	}

}
