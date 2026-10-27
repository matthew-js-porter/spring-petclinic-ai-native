package org.springframework.samples.petclinic;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

// @AutoConfigureMockMvc so this shares the cached context of the other full-context tests
@SpringBootTest
@AutoConfigureMockMvc
class PetClinicApplicationTests {

	@Test
	void contextLoads() {
	}

}
