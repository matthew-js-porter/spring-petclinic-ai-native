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

	private static final Sort BY_ID = Sort.by("id");

	private static final Comparator<PetView> BY_NAME = Comparator.comparing(PetView::name)
		.thenComparing(PetView::id);

	private final OwnerRepository owners;

	OwnerController(OwnerRepository owners) {
		this.owners = owners;
	}

	@GetMapping("/owners.html")
	String showOwnerList(@RequestParam(defaultValue = "1") int page, Model model) {
		if (page < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 1 or greater");
		}
		Page<Owner> result = this.owners.findAll(PageRequest.of(page - 1, PAGE_SIZE, BY_ID));
		if (page > 1 && page > result.getTotalPages()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "page " + page + " does not exist");
		}
		model.addAttribute("listOwners", result.map(OwnerController::toView));
		return "owners/ownersList";
	}

	@GetMapping(path = "/owners", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	Owners showResourcesOwnerList() {
		return new Owners(this.owners.findAll(BY_ID).stream().map(OwnerController::toView).toList());
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
