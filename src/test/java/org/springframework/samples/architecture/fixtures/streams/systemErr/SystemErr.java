package org.springframework.samples.architecture.fixtures.streams.systemErr;

/**
 * Writes to {@code System.err}.
 */
class SystemErr {

	void print(String text) {
		System.err.println(text);
	}

}
