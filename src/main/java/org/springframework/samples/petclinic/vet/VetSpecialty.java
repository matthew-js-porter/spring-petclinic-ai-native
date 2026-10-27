package org.springframework.samples.petclinic.vet;

import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Row of the {@code vet_specialties} join table, owned by {@link Vet}. Specialties are
 * their own aggregate, so they are referenced by id.
 * <p>
 * Spring Data JDBC quotes explicit table and column names, so they are written in upper
 * case to match how H2 stores the unquoted names from {@code schema.sql}.
 *
 * @param specialty the specialty
 * @author Matthew Porter
 */
@Table("VET_SPECIALTIES")
public record VetSpecialty(@Column("SPECIALTY_ID") AggregateReference<Specialty, Integer> specialty) {
}
