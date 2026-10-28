package org.springframework.samples.architecture.fixtures.bfpp.nonstatic;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares a {@code BeanFactoryPostProcessor} with a non-static {@code @Bean} method.
 */
@Configuration(proxyBeanMethods = false)
class NonStaticBeanFactoryPostProcessorConfiguration {

	@Bean
	BeanFactoryPostProcessor postProcessor() {
		return (beanFactory) -> {
		};
	}

}
