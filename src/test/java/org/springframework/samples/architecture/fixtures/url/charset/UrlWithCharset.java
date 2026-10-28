package org.springframework.samples.architecture.fixtures.url.charset;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Encodes and decodes with a {@code Charset}.
 */
class UrlWithCharset {

	String encode(String text) {
		return URLEncoder.encode(text, StandardCharsets.UTF_8);
	}

	String decode(String text) {
		return URLDecoder.decode(text, StandardCharsets.UTF_8);
	}

}
