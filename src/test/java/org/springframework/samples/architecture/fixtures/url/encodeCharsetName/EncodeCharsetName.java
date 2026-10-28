package org.springframework.samples.architecture.fixtures.url.encodeCharsetName;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Encodes with a charset named by a {@code String}.
 */
class EncodeCharsetName {

	String encode(String text) throws UnsupportedEncodingException {
		return URLEncoder.encode(text, "UTF-8");
	}

}
