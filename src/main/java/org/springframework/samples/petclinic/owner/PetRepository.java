package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

/**
 * The narrow write surface Add Pet and Edit Pet need. Pets are read through their
 * {@link Owner}. {@link Pet} has no owner field, because {@code OWNER_ID} is the id
 * column of the owner's pet collection, so saving a pet on its own cannot set it. Saving
 * an existing owner deletes and re-inserts its pets, which renumbers them. So both writes
 * are explicit queries, and this interface extends {@link Repository} so that no other
 * write method exists.
 *
 * @author Matthew Porter
 */
public interface PetRepository extends Repository<Pet, Integer> {

	/**
	 * Adds a pet to an owner; the id comes from the table's identity.
	 * @param ownerId the owner's id
	 * @param name the pet's name
	 * @param birthDate the date of birth
	 * @param typeId the id of the pet's type
	 */
	@Modifying
	@Query("INSERT INTO PETS (NAME, BIRTH_DATE, TYPE_ID, OWNER_ID) VALUES (:name, :birthDate, :typeId, :ownerId)")
	void insert(Integer ownerId, String name, LocalDate birthDate, Integer typeId);

	/**
	 * Changes a pet's name, birth date and type. The owner is part of the match, so a pet
	 * can only be changed through its own owner and never moves to another.
	 * @param ownerId the owner's id
	 * @param id the pet's id
	 * @param name the new name
	 * @param birthDate the new date of birth
	 * @param typeId the id of the new type
	 * @return {@code false} if that owner has no pet with that id
	 */
	@Modifying
	@Query("UPDATE PETS SET NAME = :name, BIRTH_DATE = :birthDate, TYPE_ID = :typeId "
			+ "WHERE ID = :id AND OWNER_ID = :ownerId")
	boolean update(Integer ownerId, Integer id, String name, LocalDate birthDate, Integer typeId);

}
