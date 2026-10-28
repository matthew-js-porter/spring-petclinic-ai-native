package org.springframework.samples.architecture.fixtures.string.toLowerCase;

/**
 * Calls {@code toLowerCase()} with the default locale.
 */
class ToLowerCase {

	String convert(String text) {
		return text.toLowerCase();
	}

}
