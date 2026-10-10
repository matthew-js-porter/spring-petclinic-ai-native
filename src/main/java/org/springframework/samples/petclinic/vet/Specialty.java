package org.springframework.samples.petclinic.vet;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("SPECIALTIES")
public record Specialty(@Id Integer id, String name) {
}
