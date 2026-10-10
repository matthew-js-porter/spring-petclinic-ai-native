package org.springframework.samples.petclinic.owner;

import java.util.List;

/**
 * An owner as rendered on the page and in JSON, with their pets sorted by name.
 */
public record OwnerView(Integer id, String firstName, String lastName, String address, String city,
		String telephone, List<PetView> pets) {
}
