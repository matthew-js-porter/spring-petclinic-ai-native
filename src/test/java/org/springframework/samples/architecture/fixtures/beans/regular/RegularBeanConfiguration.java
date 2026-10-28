package org.springframework.samples.architecture.fixtures.beans.regular;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares a {@code @Bean} method that returns a package-private type.
 */
@Configuration(proxyBeanMethods = false)
class RegularBeanConfiguration {

	@Bean
	Visible visible() {
		return new Visible();
	}

	static final class Visible {

	}

}
