package org.springframework.samples.architecture.fixtures.url.decodePlatformCharset;

import java.net.URLDecoder;

/**
 * Decodes with the platform charset.
 */
class DecodePlatformCharset {

	@SuppressWarnings("deprecation")
	String decode(String text) {
		return URLDecoder.decode(text);
	}

}
