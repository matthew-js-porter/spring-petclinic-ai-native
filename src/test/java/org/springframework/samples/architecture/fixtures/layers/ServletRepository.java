package org.springframework.samples.architecture.fixtures.layers;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.repository.Repository;

/**
 * A repository that uses the servlet API.
 */
interface ServletRepository extends Repository<LayersEntity, Integer> {

	void save(HttpServletRequest request);

}
