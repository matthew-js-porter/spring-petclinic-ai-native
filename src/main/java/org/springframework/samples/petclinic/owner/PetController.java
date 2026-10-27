package org.springframework.samples.petclinic.owner;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * New Pet and Edit Pet. Pets are reached through their owner: an unknown owner, an
 * unknown pet, or a pet of another owner is 404 before anything is written.
 *
 * @author Matthew Porter
 */
@Controller
class PetController {

	private static final String FORM_VIEW = "owners/petForm";

	private static final Comparator<PetType> TYPE_BY_NAME = Comparator.comparing(PetType::name);

	private final OwnerRepository owners;

	private final PetTypeRepository petTypes;

	private final PetRepository pets;

	private final Clock clock;

	PetController(OwnerRepository owners, PetTypeRepository petTypes, PetRepository pets, Clock clock) {
		this.owners = owners;
		this.petTypes = petTypes;
		this.pets = pets;
		this.clock = clock;
	}

	/**
	 * Trims the form's values before validation; an empty value stays {@code ""} so it
	 * fails {@code @NotBlank} and is redisplayed as typed.
	 * @param binder the binder for the form
	 */
	@InitBinder("petForm")
	void trimPetForm(WebDataBinder binder) {
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
	}

	@GetMapping("/owners/{ownerId}/pets/new.html")
	String initCreationForm(@PathVariable Integer ownerId, Model model) {
		Owner owner = findOwner(ownerId);
		model.addAttribute("petForm", PetForm.empty());
		return form(model, owner, null, sortedTypes());
	}

	@PostMapping("/owners/{ownerId}/pets")
	ModelAndView processCreationForm(@PathVariable Integer ownerId, @Valid @ModelAttribute("petForm") PetForm form,
			BindingResult result, Model model, RedirectAttributes flash) {
		Owner owner = findOwner(ownerId);
		List<PetType> types = sortedTypes();
		checkContext(form, result, owner, null, types);
		if (result.hasErrors()) {
			return new ModelAndView(form(model, owner, null, types));
		}
		this.pets.insert(ownerId, form.name(), IsoDates.parse(form.birthDate()).orElseThrow(),
				Integer.valueOf(form.typeId()));
		flash.addFlashAttribute("message", "New Pet has been Added");
		return seeOther(ownerId);
	}

	@GetMapping("/owners/{ownerId}/pets/{petId}/edit.html")
	String initUpdateForm(@PathVariable Integer ownerId, @PathVariable Integer petId, Model model) {
		Owner owner = findOwner(ownerId);
		model.addAttribute("petForm", PetForm.from(findPet(owner, petId)));
		return form(model, owner, petId, sortedTypes());
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}")
	ModelAndView processUpdateForm(@PathVariable Integer ownerId, @PathVariable Integer petId,
			@Valid @ModelAttribute("petForm") PetForm form, BindingResult result, Model model,
			RedirectAttributes flash) {
		Owner owner = findOwner(ownerId);
		findPet(owner, petId);
		List<PetType> types = sortedTypes();
		checkContext(form, result, owner, petId, types);
		if (result.hasErrors()) {
			return new ModelAndView(form(model, owner, petId, types));
		}
		if (!this.pets.update(ownerId, petId, form.name(), IsoDates.parse(form.birthDate()).orElseThrow(),
				Integer.valueOf(form.typeId()))) {
			throw petNotFound(petId);
		}
		flash.addFlashAttribute("message", "Pet details has been edited");
		return seeOther(ownerId);
	}

	/**
	 * The checks that need more than the value itself: the date's format and today's
	 * date, the stored types, and the owner's other pets. A field that already failed
	 * {@code @NotBlank} or {@code @Size} gets no second message.
	 * @param form the bound form
	 * @param result the binding result to add errors to
	 * @param owner the pet's owner
	 * @param petId the pet's id, or {@code null} for New Pet
	 * @param types the stored pet types
	 */
	private void checkContext(PetForm form, BindingResult result, Owner owner, Integer petId, List<PetType> types) {
		if (!result.hasFieldErrors("birthDate")) {
			Optional<LocalDate> birthDate = IsoDates.parse(form.birthDate());
			if (birthDate.isEmpty()) {
				result.rejectValue("birthDate", "format", "Birth Date must be a date in the form yyyy-mm-dd.");
			}
			else if (birthDate.get().isAfter(LocalDate.now(this.clock))) {
				result.rejectValue("birthDate", "future", "Birth Date cannot be in the future.");
			}
		}
		if (!result.hasFieldErrors("typeId") && !isKnownType(form.typeId(), types)) {
			result.rejectValue("typeId", "unknown", "Type is required.");
		}
		if (!result.hasFieldErrors("name") && owner.pets()
			.stream()
			.anyMatch((pet) -> !Objects.equals(pet.id(), petId) && pet.name().equalsIgnoreCase(form.name()))) {
			result.rejectValue("name", "duplicate", "This owner already has a pet with this name.");
		}
	}

	private static boolean isKnownType(String typeId, List<PetType> types) {
		try {
			int id = Integer.parseInt(typeId);
			return types.stream().anyMatch((type) -> type.id() == id);
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

	private List<PetType> sortedTypes() {
		return this.petTypes.findAll().stream().sorted(TYPE_BY_NAME).toList();
	}

	private Owner findOwner(Integer ownerId) {
		return this.owners.findById(ownerId)
			.orElseThrow(
					() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner " + ownerId + " does not exist"));
	}

	/**
	 * Finds the pet among the owner's own, so a pet of another owner is as unknown as one
	 * that does not exist.
	 * @param owner the owner
	 * @param petId the pet's id
	 * @return the pet
	 */
	private static Pet findPet(Owner owner, Integer petId) {
		return owner.pets()
			.stream()
			.filter((pet) -> pet.id().equals(petId))
			.findFirst()
			.orElseThrow(() -> petNotFound(petId));
	}

	private static ResponseStatusException petNotFound(Integer petId) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "pet " + petId + " does not exist");
	}

	/**
	 * Fills in what differs between New Pet ({@code petId} null) and Edit Pet.
	 * @param model the model to fill
	 * @param owner the pet's owner
	 * @param petId the pet's id, or {@code null} for New Pet
	 * @param types the pet types to offer, sorted by name
	 * @return the form view
	 */
	private String form(Model model, Owner owner, Integer petId, List<PetType> types) {
		boolean create = petId == null;
		model.addAttribute("ownerName", owner.firstName() + " " + owner.lastName());
		model.addAttribute("petTypes", types);
		model.addAttribute("today", LocalDate.now(this.clock).toString());
		model.addAttribute("heading", create ? "New Pet" : "Edit Pet");
		model.addAttribute("submitLabel", create ? "Add Pet" : "Update Pet");
		model.addAttribute("action",
				create ? "/owners/" + owner.id() + "/pets" : "/owners/" + owner.id() + "/pets/" + petId);
		return FORM_VIEW;
	}

	/**
	 * Post/redirect/get to the owner's details page with 303 See Other, which tells the
	 * browser to follow with a GET.
	 * @param ownerId the owner's id
	 * @return the redirect
	 */
	private static ModelAndView seeOther(Integer ownerId) {
		RedirectView redirect = new RedirectView("/owners/" + ownerId + ".html", true);
		redirect.setStatusCode(HttpStatus.SEE_OTHER);
		redirect.setExposeModelAttributes(false);
		return new ModelAndView(redirect);
	}

}
