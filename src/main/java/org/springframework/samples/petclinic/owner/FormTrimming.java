package org.springframework.samples.petclinic.owner;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Trims the owner, pet and visit forms' values before validation; an empty value stays
 * {@code ""} so it fails {@code @NotBlank} and is redisplayed as typed.
 */
@ControllerAdvice(assignableTypes = { OwnerController.class, PetController.class, VisitController.class })
class FormTrimming {

	@InitBinder({ "ownerForm", "petForm", "visitForm" })
	void trim(WebDataBinder binder) {
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
	}

}
