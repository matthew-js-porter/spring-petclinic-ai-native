package org.springframework.samples.architecture.fixtures.repositories.unnamed;

import org.springframework.data.repository.Repository;
import org.springframework.samples.architecture.fixtures.repositories.named.NamedEntity;

/**
 * A repository not named {@code *Repository}.
 */
interface Store extends Repository<NamedEntity, Integer> {

}
