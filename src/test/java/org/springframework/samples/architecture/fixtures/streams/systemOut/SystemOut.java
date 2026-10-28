package org.springframework.samples.architecture.fixtures.streams.systemOut;

/**
 * Writes to {@code System.out}.
 */
class SystemOut {

	void print(String text) {
		System.out.println(text);
	}

}
