package org.springframework.samples.petclinic.owner;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

/**
 * Read-only access to pet types, used to show a pet's type by name.
 */
public interface PetTypeRepository extends Repository<PetType, Integer> {

	List<PetType> findAll();

	Optional<PetType> findById(Integer id);

}
