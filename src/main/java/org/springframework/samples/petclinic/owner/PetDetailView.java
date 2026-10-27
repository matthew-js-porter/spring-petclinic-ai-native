package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A pet as rendered on the owner details page and in its JSON: name, birth date, the name
 * of its type, and its visits, most recent first.
 *
 * @param id the pet's id
 * @param name the pet's name
 * @param birthDate the date of birth
 * @param type the name of the pet's type
 * @param visits the pet's visits, most recent first
 * @author Matthew Porter
 */
public record PetDetailView(Integer id, String name, LocalDate birthDate, @Nullable String type,
		List<VisitView> visits) {
}
