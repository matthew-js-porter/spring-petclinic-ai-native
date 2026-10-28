package org.springframework.samples.architecture.fixtures.injection.constructor;

/**
 * Takes its collaborator through the constructor.
 */
class ConstructorInjected {

	private final Object collaborator;

	ConstructorInjected(Object collaborator) {
		this.collaborator = collaborator;
	}

	Object collaborator() {
		return this.collaborator;
	}

}
