package org.springframework.samples.petclinic.vet;

import java.util.List;

/**
 * JSON body of {@code GET /vets}; keeps upstream PetClinic's {@code vetList} wrapper.
 *
 * @param vetList the vets, in id order
 * @author Matthew Porter
 */
public record Vets(List<VetView> vetList) {
}
