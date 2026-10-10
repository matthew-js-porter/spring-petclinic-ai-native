package org.springframework.samples.petclinic.vet;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("vets")
public record Vet(@Id Integer id, String firstName, String lastName,
		@MappedCollection(idColumn = "vet_id") Set<VetSpecialty> specialties) {
}
