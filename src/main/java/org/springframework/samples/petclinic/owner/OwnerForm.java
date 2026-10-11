package org.springframework.samples.petclinic.owner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What the New Owner and Edit Owner forms bind: the five editable fields and nothing
 * else, so a request cannot set an owner's id or pets. Values arrive trimmed.
 */
record OwnerForm(
		@NotBlank(message = "First Name is required.") @Size(max = 30,
				message = "First Name must be 30 characters or fewer.") String firstName,
		@NotBlank(message = "Last Name is required.") @Size(max = 30,
				message = "Last Name must be 30 characters or fewer.") String lastName,
		@NotBlank(message = "Address is required.") @Size(max = 255,
				message = "Address must be 255 characters or fewer.") String address,
		@NotBlank(message = "City is required.") @Size(max = 80,
				message = "City must be 80 characters or fewer.") String city,
		// The group is optional so an empty value fails only @NotBlank. \d is ASCII-only
		// because UNICODE_CHARACTER_CLASS is not set.
		@NotBlank(message = "Telephone is required.") @Pattern(regexp = "(\\d{10})?",
				message = "Telephone must be exactly 10 digits.") String telephone) {

	static OwnerForm empty() {
		return new OwnerForm("", "", "", "", "");
	}

	static OwnerForm from(Owner owner) {
		return new OwnerForm(owner.firstName(), owner.lastName(), owner.address(), owner.city(), owner.telephone());
	}

}
