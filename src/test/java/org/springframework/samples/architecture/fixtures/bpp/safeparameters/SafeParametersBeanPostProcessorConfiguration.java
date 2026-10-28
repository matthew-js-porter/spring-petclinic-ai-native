package org.springframework.samples.architecture.fixtures.bpp.safeparameters;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

/**
 * Declares a static {@code BeanPostProcessor} {@code @Bean} method whose parameters do
 * not cause early initialization.
 */
@Configuration(proxyBeanMethods = false)
class SafeParametersBeanPostProcessorConfiguration {

	@Bean
	static BeanPostProcessor postProcessor(ObjectProvider<DataSource> dataSource, @Lazy DataSource lazyDataSource,
			ApplicationContext context, Environment environment, InfrastructureBean infrastructure) {
		return new BeanPostProcessor() {
		};
	}

}
