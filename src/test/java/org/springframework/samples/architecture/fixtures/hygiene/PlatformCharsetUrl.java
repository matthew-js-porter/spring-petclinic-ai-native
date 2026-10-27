package org.springframework.samples.architecture.fixtures.hygiene;

import java.net.URLEncoder;

/**
 * Encodes with the platform charset.
 */
class PlatformCharsetUrl {

	@SuppressWarnings("deprecation")
	String encode(String text) {
		return URLEncoder.encode(text);
	}

}
