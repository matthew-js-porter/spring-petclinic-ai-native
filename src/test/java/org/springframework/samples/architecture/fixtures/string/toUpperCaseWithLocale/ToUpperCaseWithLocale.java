package org.springframework.samples.architecture.fixtures.string.toUpperCaseWithLocale;

import java.util.Locale;

/**
 * Calls {@code toUpperCase(Locale)}.
 */
class ToUpperCaseWithLocale {

	String convert(String text) {
		return text.toUpperCase(Locale.ROOT);
	}

}
