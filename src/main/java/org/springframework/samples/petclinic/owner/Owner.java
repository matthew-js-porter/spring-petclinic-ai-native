package org.springframework.samples.petclinic.owner;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("OWNERS")
public record Owner(@Id Integer id, String firstName, String lastName, String address, String city, String telephone,
		@MappedCollection(idColumn = "OWNER_ID") Set<Pet> pets) {
}
