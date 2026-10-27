package org.springframework.samples.architecture.fixtures.layers;

import org.springframework.data.repository.Repository;

/**
 * A repository that depends on a controller.
 */
interface LeakyRepository extends Repository<LayersEntity, Integer> {

	void refresh(LayersController controller);

}
