package org.springframework.samples.petclinic.owner;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

/**
 * Access to owners: reads, plus the narrow write surface Add Owner and Edit Owner need.
 * Extends {@link Repository} rather than {@code CrudRepository} so that no other write
 * method exists.
 *
 * @author Matthew Porter
 */
public interface OwnerRepository extends Repository<Owner, Integer> {

	Page<Owner> findAll(Pageable pageable);

	List<Owner> findAll(Sort sort);

	Optional<Owner> findById(Integer id);

	Page<Owner> findByLastNameStartingWithIgnoreCase(String lastName, Pageable pageable);

	List<Owner> findByLastNameStartingWithIgnoreCase(String lastName, Sort sort);

	/**
	 * Inserts a new owner and returns it with its generated id. Only for an owner with a
	 * {@code null} id and no pets: saving an existing aggregate deletes and re-inserts
	 * its pets, which renumbers them. Use {@link #update} for existing owners.
	 * @param owner the new owner, without an id or pets
	 * @return the saved owner with its generated id
	 */
	Owner save(Owner owner);

	/**
	 * Changes an existing owner's five columns and leaves their pets alone.
	 * @param id the owner's id
	 * @param firstName the new first name
	 * @param lastName the new last name
	 * @param address the new street address
	 * @param city the new city
	 * @param telephone the new telephone number
	 * @return {@code false} if no owner has that id
	 */
	@Modifying
	@Query("UPDATE OWNERS SET FIRST_NAME = :firstName, LAST_NAME = :lastName, ADDRESS = :address, CITY = :city, "
			+ "TELEPHONE = :telephone WHERE ID = :id")
	boolean update(Integer id, String firstName, String lastName, String address, String city, String telephone);

}
