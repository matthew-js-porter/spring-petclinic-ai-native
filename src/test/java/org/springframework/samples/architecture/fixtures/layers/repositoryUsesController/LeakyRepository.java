package org.springframework.samples.architecture.fixtures.layers.repositoryUsesController;

import org.springframework.data.repository.Repository;
import org.springframework.samples.architecture.fixtures.layers.respected.RespectedController;
import org.springframework.samples.architecture.fixtures.layers.respected.RespectedEntity;

/**
 * A repository that depends on a controller.
 */
interface LeakyRepository extends Repository<RespectedEntity, Integer> {

	void refresh(RespectedController controller);

}
