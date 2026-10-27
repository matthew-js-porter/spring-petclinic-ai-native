package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What the New Visit form binds: the date and description and nothing else, so a request
 * cannot set a visit's id, pet or owner. Values arrive trimmed. The date is text so that
 * a malformed value gets the form's own message rather than a binding error, and is
 * redisplayed as typed; {@link VisitController} parses it.
 *
 * @param date the date as typed, {@code yyyy-mm-dd}
 * @param description what the visit is for
 * @author Matthew Porter
 */
record VisitForm(@NotBlank(message = "Date is required.") String date,
		@NotBlank(message = "Description is required.") @Size(max = 255,
				message = "Description must be 255 characters or fewer.") String description) {

	static VisitForm on(LocalDate date) {
		return new VisitForm(date.toString(), "");
	}

}
