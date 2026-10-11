package org.springframework.samples.petclinic.owner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What the New Pet and Edit Pet forms bind: the three editable fields and nothing else, so
 * a request cannot set a pet's id or owner. Values arrive trimmed. The birth date and type
 * id are text so that a malformed value gets the form's own message rather than a binding
 * error, and is redisplayed as typed; {@link PetController} parses them.
 */
record PetForm(
		@NotBlank(message = "Name is required.") @Size(max = 30,
				message = "Name must be 30 characters or fewer.") String name,
		@NotBlank(message = "Birth Date is required.") String birthDate,
		@NotBlank(message = "Type is required.") String typeId) {

	static PetForm empty() {
		return new PetForm("", "", "");
	}

	static PetForm from(Pet pet) {
		return new PetForm(pet.name(), pet.birthDate().toString(), String.valueOf(pet.type().getId()));
	}

}
