package org.springframework.samples.petclinic.owner;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * The lookups and redirect shared by the pages reached through an owner: an unknown
 * owner, an unknown pet, or a pet of another owner is 404.
 */
final class OwnerPages {

	private OwnerPages() {
	}

	static Owner findOwner(OwnerRepository owners, Integer ownerId) {
		return owners.findById(ownerId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner " + ownerId + " does not exist"));
	}

	/**
	 * Finds the pet among the owner's own, so a pet of another owner is as unknown as one
	 * that does not exist.
	 */
	static Pet findPet(Owner owner, Integer petId) {
		return owner.pets()
			.stream()
			.filter((pet) -> pet.id().equals(petId))
			.findFirst()
			.orElseThrow(() -> petNotFound(petId));
	}

	static ResponseStatusException petNotFound(Integer petId) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "pet " + petId + " does not exist");
	}

	/**
	 * Post/redirect/get to the owner's details page with 303 See Other, which tells the
	 * browser to follow with a GET.
	 */
	static ModelAndView seeOther(Integer ownerId) {
		RedirectView redirect = new RedirectView("/owners/" + ownerId + ".html", true);
		redirect.setStatusCode(HttpStatus.SEE_OTHER);
		redirect.setExposeModelAttributes(false);
		return new ModelAndView(redirect);
	}

}
