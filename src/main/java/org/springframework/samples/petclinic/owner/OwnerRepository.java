package org.springframework.samples.petclinic.owner;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.Repository;

/**
 * Read-only access to owners: extends {@link Repository} rather than
 * {@code CrudRepository} so that no write methods exist.
 */
public interface OwnerRepository extends Repository<Owner, Integer> {

	Page<Owner> findAll(Pageable pageable);

	List<Owner> findAll(Sort sort);

	Page<Owner> findByLastNameStartingWithIgnoreCase(String lastName, Pageable pageable);

	List<Owner> findByLastNameStartingWithIgnoreCase(String lastName, Sort sort);

}
