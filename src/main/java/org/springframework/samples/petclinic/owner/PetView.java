package org.springframework.samples.petclinic.owner;

/**
 * A pet as rendered on the owners page and in JSON: its name only.
 */
public record PetView(Integer id, String name) {
}
