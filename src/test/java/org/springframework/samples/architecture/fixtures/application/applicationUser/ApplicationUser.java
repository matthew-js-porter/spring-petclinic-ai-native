package org.springframework.samples.architecture.fixtures.application.applicationUser;

import org.springframework.samples.petclinic.PetClinicApplication;

/**
 * Depends on the application class.
 */
class ApplicationUser {

	Class<?> application = PetClinicApplication.class;

}
