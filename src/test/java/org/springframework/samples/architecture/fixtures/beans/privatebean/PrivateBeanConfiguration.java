package org.springframework.samples.architecture.fixtures.beans.privatebean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares a {@code @Bean} method that returns a private type.
 */
@Configuration(proxyBeanMethods = false)
class PrivateBeanConfiguration {

	@Bean
	Hidden hidden() {
		return new Hidden();
	}

	private static final class Hidden {

	}

}
