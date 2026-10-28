package org.springframework.samples.architecture.fixtures.layers.respected;

import org.springframework.data.repository.Repository;

/**
 * A repository that uses only the domain.
 */
public interface RespectedRepository extends Repository<RespectedEntity, Integer> {

	RespectedEntity findById(Integer id);

}
