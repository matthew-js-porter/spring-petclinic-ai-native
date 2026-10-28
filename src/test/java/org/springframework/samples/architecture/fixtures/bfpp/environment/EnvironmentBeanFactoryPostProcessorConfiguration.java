package org.springframework.samples.architecture.fixtures.bfpp.environment;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Declares a static {@code BeanFactoryPostProcessor} {@code @Bean} method that injects
 * only the {@code Environment}.
 */
@Configuration(proxyBeanMethods = false)
class EnvironmentBeanFactoryPostProcessorConfiguration {

	@Bean
	static BeanFactoryPostProcessor postProcessor(Environment environment) {
		return (beanFactory) -> {
		};
	}

}
