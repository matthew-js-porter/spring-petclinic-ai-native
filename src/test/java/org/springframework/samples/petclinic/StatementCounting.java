package org.springframework.samples.petclinic;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Wraps the {@link DataSource} so a test can count the statements its repository calls
 * prepare: {@link #reset()}, run the call, then read {@link #count()}.
 */
@TestConfiguration
public class StatementCounting {

	private static final AtomicInteger statements = new AtomicInteger();

	public static void reset() {
		statements.set(0);
	}

	public static int count() {
		return statements.get();
	}

	@Bean
	static BeanPostProcessor countingDataSource() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				return (bean instanceof DataSource dataSource) ? counting(dataSource) : bean;
			}
		};
	}

	private static DataSource counting(DataSource target) {
		return (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
				new Class<?>[] { DataSource.class }, (proxy, method, args) -> {
					Object result = method.invoke(target, args);
					return (result instanceof Connection connection) ? counting(connection) : result;
				});
	}

	private static Connection counting(Connection target) {
		return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
				new Class<?>[] { Connection.class }, (proxy, method, args) -> {
					if (method.getName().startsWith("prepare") || method.getName().equals("createStatement")) {
						statements.incrementAndGet();
					}
					return method.invoke(target, args);
				});
	}

}
