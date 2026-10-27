package org.springframework.samples.architecture.fixtures.hygiene;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares a {@code @Bean} method that returns a private type.
 */
@Configuration(proxyBeanMethods = false)
class PrivateBeanType {

	@Bean
	Hidden hidden() {
		return new Hidden();
	}

	private static final class Hidden {

	}

}
