package org.springframework.samples.petclinic.owner;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
class OwnerController {

	static final int PAGE_SIZE = 5;

	static final int MAX_LAST_NAME = 30;

	private static final String FORM_VIEW = "owners/ownerForm";

	private static final Sort BY_ID = Sort.by("id");

	private static final Comparator<PetView> BY_NAME = Comparator.comparing(PetView::name).thenComparing(PetView::id);

	private static final Comparator<PetDetailView> DETAIL_BY_NAME = Comparator.comparing(PetDetailView::name)
		.thenComparing(PetDetailView::id);

	private final OwnerRepository owners;

	private final PetTypeRepository petTypes;

	private final VisitRepository visits;

	OwnerController(OwnerRepository owners, PetTypeRepository petTypes, VisitRepository visits) {
		this.owners = owners;
		this.petTypes = petTypes;
		this.visits = visits;
	}

	/**
	 * Trims the form's values before validation; an empty value stays {@code ""} so it
	 * fails {@code @NotBlank} and is redisplayed as typed.
	 * @param binder the binder for the form
	 */
	@InitBinder("ownerForm")
	void trimOwnerForm(WebDataBinder binder) {
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
	}

	@GetMapping("/owners.html")
	String showOwnerList(@RequestParam(defaultValue = "1") int page,
			@RequestParam(required = false) @Nullable String lastName, Model model) {
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
	Owners showResourcesOwnerList(@RequestParam(required = false) @Nullable String lastName) {
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

	@GetMapping("/owners/new.html")
	String initCreationForm(Model model) {
		model.addAttribute("ownerForm", OwnerForm.empty());
		return form(model, null);
	}

	@PostMapping("/owners")
	ModelAndView processCreationForm(@Valid @ModelAttribute("ownerForm") OwnerForm form, BindingResult result,
			Model model, RedirectAttributes flash) {
		if (result.hasErrors()) {
			return new ModelAndView(form(model, null));
		}
		Owner saved = this.owners.save(new Owner(null, form.firstName(), form.lastName(), form.address(), form.city(),
				form.telephone(), Set.of()));
		flash.addFlashAttribute("message", "New Owner Created");
		return seeOther(saved.savedId());
	}

	@GetMapping("/owners/{id}/edit.html")
	String initUpdateForm(@PathVariable Integer id, Model model) {
		model.addAttribute("ownerForm", OwnerForm.from(findOwner(id)));
		return form(model, id);
	}

	@PostMapping("/owners/{id}")
	ModelAndView processUpdateForm(@PathVariable Integer id, @Valid @ModelAttribute("ownerForm") OwnerForm form,
			BindingResult result, Model model, RedirectAttributes flash) {
		findOwner(id);
		if (result.hasErrors()) {
			return new ModelAndView(form(model, id));
		}
		this.owners.update(id, form.firstName(), form.lastName(), form.address(), form.city(), form.telephone());
		flash.addFlashAttribute("message", "Owner Values Updated");
		return seeOther(id);
	}

	/**
	 * One owner as JSON whatever the {@code Accept} header: no {@code produces}
	 * condition, and a preset content type that Spring writes without negotiating.
	 * @param id the owner's id
	 * @return the owner's details
	 */
	@GetMapping("/owners/{id}")
	ResponseEntity<OwnerDetailView> showResourcesOwner(@PathVariable Integer id) {
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ownerDetails(id));
	}

	/**
	 * Looks up one owner for the details page and its JSON, naming each pet's type and
	 * loading all their pets' visits in one query. Unknown ids are 404.
	 * @param id the owner's id
	 * @return the owner's details
	 */
	private OwnerDetailView ownerDetails(Integer id) {
		Owner owner = findOwner(id);
		Map<Integer, String> typeNames = this.petTypes.findAll()
			.stream()
			.collect(Collectors.toMap(PetType::id, PetType::name));
		Map<Integer, List<Visit>> visitsByPet = owner.pets().isEmpty() ? Map.of()
				: this.visits.findByPetIds(owner.pets().stream().map(Pet::id).toList())
					.stream()
					.collect(Collectors.groupingBy(Visit::petId));
		List<PetDetailView> pets = owner.pets()
			.stream()
			.map((pet) -> new PetDetailView(pet.id(), pet.name(), pet.birthDate(), typeNames.get(pet.type().getId()),
					VisitView.mostRecentFirst(visitsByPet.getOrDefault(pet.id(), List.of()))))
			.sorted(DETAIL_BY_NAME)
			.toList();
		return new OwnerDetailView(owner.savedId(), owner.firstName(), owner.lastName(), owner.address(), owner.city(),
				owner.telephone(), pets);
	}

	private Owner findOwner(Integer id) {
		return this.owners.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner " + id + " does not exist"));
	}

	/**
	 * Fills in what differs between New Owner ({@code ownerId} null) and Edit Owner.
	 * @param model the model to fill
	 * @param ownerId the owner's id, or {@code null} for New Owner
	 * @return the form view
	 */
	private static String form(Model model, @Nullable Integer ownerId) {
		boolean create = ownerId == null;
		model.addAttribute("ownerId", ownerId);
		model.addAttribute("heading", create ? "New Owner" : "Edit Owner");
		model.addAttribute("submitLabel", create ? "Add Owner" : "Update Owner");
		model.addAttribute("action", create ? "/owners" : "/owners/" + ownerId);
		return FORM_VIEW;
	}

	/**
	 * Post/redirect/get to the owner's details page with 303 See Other, which tells the
	 * browser to follow with a GET.
	 * @param id the owner's id
	 * @return the redirect
	 */
	private static ModelAndView seeOther(Integer id) {
		RedirectView redirect = new RedirectView("/owners/" + id + ".html", true);
		redirect.setStatusCode(HttpStatus.SEE_OTHER);
		redirect.setExposeModelAttributes(false);
		return new ModelAndView(redirect);
	}

	/**
	 * Normalises the last-name search term shared by the page and the JSON: missing
	 * becomes empty, surrounding whitespace is stripped, and anything longer than the
	 * column is rejected.
	 * @param lastName the request parameter, or {@code null} if absent
	 * @return the term to search by, empty for all owners
	 */
	private static String searchTerm(@Nullable String lastName) {
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
		return new OwnerView(owner.savedId(), owner.firstName(), owner.lastName(), owner.address(), owner.city(),
				owner.telephone(), pets);
	}

}
