package org.springframework.samples.petclinic;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;

@SpringBootApplication
public class PetClinicApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetClinicApplication.class, args);
	}

	/**
	 * Spring Data JDBC quotes table and column names by default, which makes them case
	 * sensitive and stops lower-case mapping names from matching the unquoted names in
	 * {@code schema.sql}. Unquoted, they resolve like the DDL does on every database.
	 */
	@Bean
	static BeanPostProcessor unquotedJdbcIdentifiers() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String beanName) {
				if (bean instanceof JdbcMappingContext context) {
					context.setForceQuote(false);
				}
				return bean;
			}
		};
	}

}
