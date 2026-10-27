package org.springframework.samples.petclinic.vet;

import java.util.List;

/**
 * A vet as rendered on the page and in JSON, with its specialties sorted by name.
 *
 * @param id the vet's id
 * @param firstName the first name
 * @param lastName the last name
 * @param specialties the vet's specialties, sorted by name
 * @author Matthew Porter
 */
public record VetView(Integer id, String firstName, String lastName, List<SpecialtyView> specialties) {
}
