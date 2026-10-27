package org.springframework.samples.architecture.fixtures.injection;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Injects a field.
 */
class FieldInjected {

	@Autowired
	Object collaborator;

}
