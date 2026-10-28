package org.springframework.samples.architecture.fixtures.layers.respected;

import org.springframework.stereotype.Controller;

/**
 * A web type that uses the repository, the domain and a presentation type.
 */
@Controller
public class RespectedController {

	private final RespectedRepository repository;

	RespectedController(RespectedRepository repository) {
		this.repository = repository;
	}

	RespectedView show(Integer id) {
		return RespectedView.of(this.repository.findById(id));
	}

}
