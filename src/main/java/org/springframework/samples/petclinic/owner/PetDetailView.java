package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;

/**
 * A pet as rendered on the owner details page and in its JSON: name, birth date and the
 * name of its type.
 */
public record PetDetailView(Integer id, String name, LocalDate birthDate, String type) {
}
