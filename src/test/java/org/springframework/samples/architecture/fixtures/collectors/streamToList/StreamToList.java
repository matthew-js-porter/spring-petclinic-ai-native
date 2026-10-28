package org.springframework.samples.architecture.fixtures.collectors.streamToList;

import java.util.List;
import java.util.stream.Stream;

/**
 * Collects with {@code Stream.toList()}.
 */
class StreamToList {

	List<String> collect(Stream<String> stream) {
		return stream.toList();
	}

}
