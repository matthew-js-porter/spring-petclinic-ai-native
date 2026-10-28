package org.springframework.samples.architecture.fixtures.layers.viewUsesRepository;

import org.springframework.samples.architecture.fixtures.layers.respected.RespectedRepository;

/**
 * A presentation type that depends on a repository.
 */
record LeakyView(RespectedRepository repository) {

}
