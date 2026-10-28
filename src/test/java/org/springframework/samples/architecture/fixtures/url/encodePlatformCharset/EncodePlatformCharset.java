package org.springframework.samples.architecture.fixtures.url.encodePlatformCharset;

import java.net.URLEncoder;

/**
 * Encodes with the platform charset.
 */
class EncodePlatformCharset {

	@SuppressWarnings("deprecation")
	String encode(String text) {
		return URLEncoder.encode(text);
	}

}
