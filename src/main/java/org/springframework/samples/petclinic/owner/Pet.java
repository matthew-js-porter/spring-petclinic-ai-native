package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A pet, owned by {@link Owner}. {@code birthDate} and {@code type} are shown on the owner
 * details page only, not on the owners list.
 * <p>
 * Spring Data JDBC quotes explicit table and column names, so they are written in upper
 * case to match how H2 stores the unquoted names from {@code schema.sql}.
 */
@Table("PETS")
public record Pet(@Id Integer id, String name, LocalDate birthDate,
		@Column("TYPE_ID") AggregateReference<PetType, Integer> type) {
}
