package org.springframework.samples.petclinic.owner;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	private static final Comparator<PetDetailView> DETAIL_BY_NAME = Comparator.comparing(PetDetailView::name)
		.thenComparing(PetDetailView::id);

	private final OwnerRepository owners;

	private final PetTypeRepository petTypes;

	OwnerController(OwnerRepository owners, PetTypeRepository petTypes) {
		this.owners = owners;
		this.petTypes = petTypes;
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

	@GetMapping("/owners/{id}.html")
	String showOwner(@PathVariable Integer id, Model model) {
		model.addAttribute("owner", ownerDetails(id));
		return "owners/ownerDetails";
	}

	/**
	 * One owner as JSON whatever the {@code Accept} header: no {@code produces} condition,
	 * and a preset content type that Spring writes without negotiating.
	 */
	@GetMapping("/owners/{id}")
	ResponseEntity<OwnerDetailView> showResourcesOwner(@PathVariable Integer id) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ownerDetails(id));
	}

	/**
	 * Looks up one owner for the details page and its JSON, naming each pet's type.
	 * Unknown ids are 404.
	 */
	private OwnerDetailView ownerDetails(Integer id) {
		Owner owner = this.owners.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner " + id + " does not exist"));
		Map<Integer, String> typeNames = this.petTypes.findAll()
			.stream()
			.collect(Collectors.toMap(PetType::id, PetType::name));
		List<PetDetailView> pets = owner.pets()
			.stream()
			.map((pet) -> new PetDetailView(pet.id(), pet.name(), pet.birthDate(), typeNames.get(pet.type().getId())))
			.sorted(DETAIL_BY_NAME)
			.toList();
		return new OwnerDetailView(owner.id(), owner.firstName(), owner.lastName(), owner.address(), owner.city(),
				owner.telephone(), pets);
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
