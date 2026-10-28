package org.springframework.samples.architecture.fixtures.bfpp.parameters;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares a static {@code BeanFactoryPostProcessor} {@code @Bean} method that injects
 * a bean.
 */
@Configuration(proxyBeanMethods = false)
class ParametersBeanFactoryPostProcessorConfiguration {

	@Bean
	static BeanFactoryPostProcessor postProcessor(DataSource dataSource) {
		return (beanFactory) -> {
		};
	}

}
