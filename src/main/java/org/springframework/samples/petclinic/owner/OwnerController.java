package org.springframework.samples.petclinic.owner;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
class OwnerController {

	static final int PAGE_SIZE = 5;

	static final int MAX_LAST_NAME = 30;

	private static final Sort BY_ID = Sort.by("id");

	private static final Comparator<PetView> BY_NAME = Comparator.comparing(PetView::name)
		.thenComparing(PetView::id);

	private final OwnerRepository owners;

	OwnerController(OwnerRepository owners) {
		this.owners = owners;
	}

	@GetMapping("/owners.html")
	String showOwnerList(@RequestParam(defaultValue = "1") int page, @RequestParam(required = false) String lastName,
			Model model) {
		if (page < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 1 or greater");
		}
		String term = searchTerm(lastName);
		PageRequest pageable = PageRequest.of(page - 1, PAGE_SIZE, BY_ID);
		Page<Owner> result = term.isEmpty() ? this.owners.findAll(pageable)
				: this.owners.findByLastNameStartingWithIgnoreCase(term, pageable);
		if (page > 1 && page > result.getTotalPages()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "page " + page + " does not exist");
		}
		model.addAttribute("listOwners", result.map(OwnerController::toView));
		model.addAttribute("lastName", term);
		return "owners/ownersList";
	}

	@GetMapping(path = "/owners", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	Owners showResourcesOwnerList(@RequestParam(required = false) String lastName) {
		String term = searchTerm(lastName);
		List<Owner> result = term.isEmpty() ? this.owners.findAll(BY_ID)
				: this.owners.findByLastNameStartingWithIgnoreCase(term, BY_ID);
		return new Owners(result.stream().map(OwnerController::toView).toList());
	}

	/**
	 * Normalises the last-name search term shared by the page and the JSON: missing
	 * becomes empty, surrounding whitespace is stripped, and anything longer than the
	 * column is rejected.
	 */
	private static String searchTerm(String lastName) {
		String term = (lastName != null) ? lastName.strip() : "";
		if (term.length() > MAX_LAST_NAME) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"lastName must be " + MAX_LAST_NAME + " characters or fewer");
		}
		return term;
	}

	private static OwnerView toView(Owner owner) {
		List<PetView> pets = owner.pets()
			.stream()
			.map((pet) -> new PetView(pet.id(), pet.name()))
			.sorted(BY_NAME)
			.toList();
		return new OwnerView(owner.id(), owner.firstName(), owner.lastName(), owner.address(), owner.city(),
				owner.telephone(), pets);
	}

}
