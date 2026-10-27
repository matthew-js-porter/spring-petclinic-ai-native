package org.springframework.samples.architecture.fixtures.layers;

import org.springframework.data.relational.core.mapping.Table;

/**
 * A domain type.
 */
@Table("LAYERS")
record LayersEntity(Integer id) {

}
