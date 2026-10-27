package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.Repository;

/**
 * Reads and adds visits. Visits sit outside the {@link Owner} aggregate: mapping them
 * onto {@link Pet} would make any re-save of an owner delete and re-insert its pets under
 * the visits that reference them. Visits are never edited or deleted, so this interface
 * extends {@link Repository} and offers only these two queries.
 */
public interface VisitRepository extends Repository<Visit, Integer> {

	/**
	 * The visits of several pets in one query, unordered. {@code petIds} must not be
	 * empty.
	 */
	@Query("SELECT * FROM VISITS WHERE PET_ID IN (:petIds)")
	List<Visit> findByPetIds(Collection<Integer> petIds);

	/**
	 * Adds a visit to a pet; the id comes from the table's identity.
	 */
	@Modifying
	@Query("INSERT INTO VISITS (PET_ID, VISIT_DATE, DESCRIPTION) VALUES (:petId, :date, :description)")
	void insert(Integer petId, LocalDate date, String description);

}
