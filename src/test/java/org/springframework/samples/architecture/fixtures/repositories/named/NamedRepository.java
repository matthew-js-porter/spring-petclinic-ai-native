package org.springframework.samples.architecture.fixtures.repositories.named;

import org.springframework.data.repository.Repository;

/**
 * A repository interface named {@code *Repository}.
 */
interface NamedRepository extends Repository<NamedEntity, Integer> {

}
