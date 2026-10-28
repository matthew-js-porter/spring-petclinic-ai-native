package org.springframework.samples.architecture.fixtures.string.toLowerCaseWithLocale;

import java.util.Locale;

/**
 * Calls {@code toLowerCase(Locale)}.
 */
class ToLowerCaseWithLocale {

	String convert(String text) {
		return text.toLowerCase(Locale.ROOT);
	}

}
