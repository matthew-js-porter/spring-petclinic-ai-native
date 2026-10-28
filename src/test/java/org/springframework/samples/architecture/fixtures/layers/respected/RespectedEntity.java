package org.springframework.samples.architecture.fixtures.layers.respected;

import org.springframework.data.relational.core.mapping.Table;

/**
 * A domain type.
 */
@Table("RESPECTED")
public record RespectedEntity(Integer id) {

}
