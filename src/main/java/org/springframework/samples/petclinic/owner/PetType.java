package org.springframework.samples.petclinic.owner;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A kind of pet (cat, dog, ...). Its own aggregate, referenced by id from {@link Pet}.
 *
 * @param id the type's id
 * @param name the type's name
 * @author Matthew Porter
 */
@Table("TYPES")
public record PetType(@Id Integer id, String name) {
}
