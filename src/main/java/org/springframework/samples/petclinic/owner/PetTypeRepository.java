package org.springframework.samples.petclinic.owner;

import java.util.List;

import org.springframework.data.repository.Repository;

/**
 * Read-only access to pet types, used to show a pet's type by name.
 *
 * @author Matthew Porter
 */
public interface PetTypeRepository extends Repository<PetType, Integer> {

	List<PetType> findAll();

}
