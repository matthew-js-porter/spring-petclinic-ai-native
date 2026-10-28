package org.springframework.samples.architecture.fixtures.url.decodeCharsetName;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Decodes with a charset named by a {@code String}.
 */
class DecodeCharsetName {

	String decode(String text) throws UnsupportedEncodingException {
		return URLDecoder.decode(text, "UTF-8");
	}

}
