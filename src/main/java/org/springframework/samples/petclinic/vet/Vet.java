package org.springframework.samples.petclinic.vet;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("VETS")
public record Vet(@Id Integer id, String firstName, String lastName,
		@MappedCollection(idColumn = "VET_ID") Set<VetSpecialty> specialties) {
}
