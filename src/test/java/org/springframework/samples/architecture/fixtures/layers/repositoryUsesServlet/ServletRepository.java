package org.springframework.samples.architecture.fixtures.layers.repositoryUsesServlet;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.repository.Repository;
import org.springframework.samples.architecture.fixtures.layers.respected.RespectedEntity;

/**
 * A repository that uses the servlet API.
 */
interface ServletRepository extends Repository<RespectedEntity, Integer> {

	void save(HttpServletRequest request);

}
