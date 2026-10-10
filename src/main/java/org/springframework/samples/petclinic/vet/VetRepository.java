package org.springframework.samples.petclinic.vet;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;

/**
 * Read-only access to vets: extends {@link Repository} rather than
 * {@code CrudRepository} so that no write methods exist.
 */
public interface VetRepository extends Repository<Vet, Integer> {

	Page<Vet> findAll(Pageable pageable);

	List<Vet> findAll(Sort sort);

}
