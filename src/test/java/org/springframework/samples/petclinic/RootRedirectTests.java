package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(RootController.class)
class RootRedirectTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void redirectsRootToVetList() {
		assertThat(this.mvc.get().uri("/")).hasStatus3xxRedirection().hasRedirectedUrl("/vets.html");
	}

}
