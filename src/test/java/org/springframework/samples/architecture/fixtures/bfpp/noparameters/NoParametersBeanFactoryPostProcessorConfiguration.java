package org.springframework.samples.architecture.fixtures.bfpp.noparameters;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares a static {@code BeanFactoryPostProcessor} {@code @Bean} method with no
 * parameters.
 */
@Configuration(proxyBeanMethods = false)
class NoParametersBeanFactoryPostProcessorConfiguration {

	@Bean
	static BeanFactoryPostProcessor postProcessor() {
		return (beanFactory) -> {
		};
	}

}
