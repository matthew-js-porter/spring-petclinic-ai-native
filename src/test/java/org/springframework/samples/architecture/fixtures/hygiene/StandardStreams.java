package org.springframework.samples.architecture.fixtures.hygiene;

/**
 * Writes to {@code System.out}.
 */
class StandardStreams {

	void print(String text) {
		System.out.println(text);
	}

}
