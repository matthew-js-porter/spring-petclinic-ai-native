package org.springframework.samples.architecture.fixtures.bpp.nonstatic;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares a {@code BeanPostProcessor} with a non-static {@code @Bean} method.
 */
@Configuration(proxyBeanMethods = false)
class NonStaticBeanPostProcessorConfiguration {

	@Bean
	BeanPostProcessor postProcessor() {
		return new BeanPostProcessor() {
		};
	}

}
