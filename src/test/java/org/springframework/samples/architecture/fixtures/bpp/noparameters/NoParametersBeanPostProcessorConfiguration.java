package org.springframework.samples.architecture.fixtures.bpp.noparameters;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares a static {@code BeanPostProcessor} {@code @Bean} method with no parameters.
 */
@Configuration(proxyBeanMethods = false)
class NoParametersBeanPostProcessorConfiguration {

	@Bean
	static BeanPostProcessor postProcessor() {
		return new BeanPostProcessor() {
		};
	}

}
