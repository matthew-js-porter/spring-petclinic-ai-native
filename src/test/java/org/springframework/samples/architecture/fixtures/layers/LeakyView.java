package org.springframework.samples.architecture.fixtures.layers;

/**
 * A presentation type that depends on a repository.
 */
record LeakyView(LeakyRepository repository) {

}
