package org.springframework.samples.petclinic.vet;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface SpecialtyRepository extends Repository<Specialty, Integer> {

	List<Specialty> findAll();

}
