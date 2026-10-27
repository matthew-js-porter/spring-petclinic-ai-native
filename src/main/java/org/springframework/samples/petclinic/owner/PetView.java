package org.springframework.samples.petclinic.owner;

/**
 * A pet as rendered on the owners page and in JSON: its name only.
 *
 * @param id the pet's id
 * @param name the pet's name
 * @author Matthew Porter
 */
public record PetView(Integer id, String name) {
}
