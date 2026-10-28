package org.springframework.samples.architecture.fixtures.bpp.unsafeparameters;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares a static {@code BeanPostProcessor} {@code @Bean} method that injects a bean
 * eagerly.
 */
@Configuration(proxyBeanMethods = false)
class UnsafeParametersBeanPostProcessorConfiguration {

	@Bean
	static BeanPostProcessor postProcessor(DataSource dataSource) {
		return new BeanPostProcessor() {
		};
	}

}
