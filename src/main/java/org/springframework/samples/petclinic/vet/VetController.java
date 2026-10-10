package org.springframework.samples.petclinic.vet;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
class VetController {

	static final int PAGE_SIZE = 5;

	private static final Sort BY_ID = Sort.by("id");

	private final VetRepository vets;

	private final SpecialtyRepository specialties;

	VetController(VetRepository vets, SpecialtyRepository specialties) {
		this.vets = vets;
		this.specialties = specialties;
	}

	@GetMapping("/vets.html")
	String showVetList(@RequestParam(defaultValue = "1") int page, Model model) {
		if (page < 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 1 or greater");
		}
		Page<Vet> result = this.vets.findAll(PageRequest.of(page - 1, PAGE_SIZE, BY_ID));
		if (page > 1 && page > result.getTotalPages()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "page " + page + " does not exist");
		}
		Function<Vet, VetView> toView = viewMapper();
		model.addAttribute("listVets", result.map(toView));
		model.addAttribute("currentPage", page);
		return "vets/vetList";
	}

	@GetMapping(path = "/vets", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	Vets showResourcesVetList() {
		return new Vets(this.vets.findAll(BY_ID).stream().map(viewMapper()).toList());
	}

	/**
	 * Resolves specialty names from one lookup of the (small) specialties table instead
	 * of one query per vet.
	 */
	private Function<Vet, VetView> viewMapper() {
		Map<Integer, Specialty> byId = this.specialties.findAll()
			.stream()
			.collect(Collectors.toMap(Specialty::id, Function.identity()));
		return (vet) -> {
			List<SpecialtyView> names = vet.specialties()
				.stream()
				.map((link) -> byId.get(link.specialty().getId()))
				.map((specialty) -> new SpecialtyView(specialty.id(), specialty.name()))
				.sorted(Comparator.comparing(SpecialtyView::name))
				.toList();
			return new VetView(vet.id(), vet.firstName(), vet.lastName(), names);
		};
	}

}
