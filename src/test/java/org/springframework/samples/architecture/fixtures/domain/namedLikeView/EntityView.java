package org.springframework.samples.architecture.fixtures.domain.namedLikeView;

import org.springframework.data.relational.core.mapping.Table;

/**
 * A domain type named like a view.
 */
@Table("ENTITY_VIEWS")
record EntityView(Integer id) {

}
