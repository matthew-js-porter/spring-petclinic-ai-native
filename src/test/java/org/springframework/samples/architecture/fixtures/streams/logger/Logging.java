package org.springframework.samples.architecture.fixtures.streams.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs through SLF4J.
 */
class Logging {

	private static final Logger logger = LoggerFactory.getLogger(Logging.class);

	void report(Exception ex) {
		logger.error("Failed", ex);
	}

}
