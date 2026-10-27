package org.springframework.samples.petclinic;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PetClinicApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetClinicApplication.class, args);
	}

	/**
	 * Today's date for checks such as "birth date not in the future"; tests replace it
	 * with a fixed clock.
	 * @return the system clock in the default time zone
	 */
	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}

}
