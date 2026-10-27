package org.springframework.samples.petclinic.owner;

import java.util.List;

/**
 * An owner as rendered on the page and in JSON, with their pets sorted by name.
 *
 * @param id the owner's id
 * @param firstName the first name
 * @param lastName the last name
 * @param address the street address
 * @param city the city
 * @param telephone the telephone number
 * @param pets the owner's pets, sorted by name
 * @author Matthew Porter
 */
public record OwnerView(Integer id, String firstName, String lastName, String address, String city, String telephone,
		List<PetView> pets) {
}
