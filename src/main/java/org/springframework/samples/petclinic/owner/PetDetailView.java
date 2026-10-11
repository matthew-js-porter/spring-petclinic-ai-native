package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.List;

/**
 * A pet as rendered on the owner details page and in its JSON: name, birth date, the name
 * of its type, and its visits, most recent first.
 */
public record PetDetailView(Integer id, String name, LocalDate birthDate, String type, List<VisitView> visits) {
}
