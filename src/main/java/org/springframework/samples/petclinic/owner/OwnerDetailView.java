package org.springframework.samples.petclinic.owner;

import java.util.List;

/**
 * One owner as rendered on the owner details page and in its JSON, with their pets
 * sorted by name.
 */
public record OwnerDetailView(Integer id, String firstName, String lastName, String address, String city,
		String telephone, List<PetDetailView> pets) {
}
