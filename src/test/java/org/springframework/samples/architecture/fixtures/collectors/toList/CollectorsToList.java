package org.springframework.samples.architecture.fixtures.collectors.toList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects with {@code Collectors.toList()}.
 */
class CollectorsToList {

	List<String> collect(Stream<String> stream) {
		return stream.collect(Collectors.toList());
	}

}
