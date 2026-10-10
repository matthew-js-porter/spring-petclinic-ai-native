package org.springframework.samples.petclinic.vet;

import java.util.List;

/**
 * JSON body of {@code GET /vets}; keeps upstream PetClinic's {@code vetList} wrapper.
 */
public record Vets(List<VetView> vetList) {
}
