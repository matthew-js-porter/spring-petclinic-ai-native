package org.springframework.samples.architecture.fixtures.stereotypes;

import org.springframework.data.relational.core.mapping.Table;

/**
 * A domain type.
 */
@Table("STEREOTYPES")
record StereotypesEntity(Integer id) {

}
