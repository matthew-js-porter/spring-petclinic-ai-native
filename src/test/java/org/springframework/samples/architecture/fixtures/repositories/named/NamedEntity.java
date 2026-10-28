package org.springframework.samples.architecture.fixtures.repositories.named;

import org.springframework.data.relational.core.mapping.Table;

/**
 * A domain type.
 */
@Table("NAMED")
public record NamedEntity(Integer id) {

}
