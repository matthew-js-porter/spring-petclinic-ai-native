package org.springframework.samples.architecture.fixtures.layers.respected;

/**
 * A presentation type that uses only the domain.
 */
record RespectedView(Integer id) {

	static RespectedView of(RespectedEntity entity) {
		return new RespectedView(entity.id());
	}

}
