package org.springframework.samples.petclinic.vet;

import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Row of the {@code vet_specialties} join table, owned by {@link Vet}. Specialties are
 * their own aggregate, so they are referenced by id.
 */
@Table("vet_specialties")
public record VetSpecialty(@Column("specialty_id") AggregateReference<Specialty, Integer> specialty) {
}
