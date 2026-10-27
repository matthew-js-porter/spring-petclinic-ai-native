package org.springframework.samples.petclinic.owner;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("OWNERS")
public record Owner(@Id @Nullable Integer id, String firstName, String lastName, String address, String city,
		String telephone, @MappedCollection(idColumn = "OWNER_ID") Set<Pet> pets) {

	/**
	 * The id of an owner read from or saved by {@link OwnerRepository}, which always has
	 * one; only an owner not yet saved has a {@code null} id.
	 * @return the generated id
	 */
	Integer savedId() {
		if (this.id == null) {
			throw new IllegalStateException("owner has not been saved");
		}
		return this.id;
	}
}
