package org.springframework.samples.architecture.fixtures.stereotypes;

import org.springframework.data.repository.Repository;

/**
 * A repository not named {@code *Repository}.
 */
interface Store extends Repository<StereotypesEntity, Integer> {

}
