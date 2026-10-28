package org.springframework.samples.architecture.fixtures.injection.autowiredField;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Injects a field.
 */
class AutowiredField {

	@Autowired
	Object collaborator;

}
