package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One visit of a pet. Read and written through {@link VisitRepository}, not through the
 * {@link Owner} aggregate, so {@link Pet} has no visits of its own.
 *
 * @param id the generated id
 * @param petId the id of the pet visited
 * @param date the date of the visit
 * @param description what the visit was for
 * @author Matthew Porter
 */
@Table("VISITS")
public record Visit(@Id Integer id, @Column("PET_ID") Integer petId, @Column("VISIT_DATE") LocalDate date,
		String description) {
}
