package org.springframework.samples.petclinic.vet;

import java.util.List;

/**
 * A vet as rendered on the page and in JSON, with its specialties sorted by name.
 */
public record VetView(Integer id, String firstName, String lastName, List<SpecialtyView> specialties) {
}
