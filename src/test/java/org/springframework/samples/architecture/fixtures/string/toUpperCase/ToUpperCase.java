package org.springframework.samples.architecture.fixtures.string.toUpperCase;

/**
 * Calls {@code toUpperCase()} with the default locale.
 */
class ToUpperCase {

	String convert(String text) {
		return text.toUpperCase();
	}

}
