package org.springframework.samples.architecture.fixtures.hygiene;

/**
 * Converts case with the default locale.
 */
class DefaultLocaleCase {

	String shout(String text) {
		return text.toUpperCase();
	}

}
