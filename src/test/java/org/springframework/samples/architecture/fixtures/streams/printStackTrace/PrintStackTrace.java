package org.springframework.samples.architecture.fixtures.streams.printStackTrace;

/**
 * Prints a stack trace.
 */
class PrintStackTrace {

	void report(Exception ex) {
		ex.printStackTrace();
	}

}
