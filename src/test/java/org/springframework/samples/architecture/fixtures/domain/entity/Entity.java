package org.springframework.samples.architecture.fixtures.domain.entity;

import org.springframework.data.relational.core.mapping.Table;

/**
 * A domain type named for the entity.
 */
@Table("ENTITIES")
record Entity(Integer id) {

}
