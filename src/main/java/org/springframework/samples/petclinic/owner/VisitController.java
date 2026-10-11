package org.springframework.samples.petclinic.owner;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

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
 * New Visit. Visits are reached through their pet's owner: an unknown owner, an unknown
 * pet, or a pet of another owner is 404 before anything is written.
 */
@Controller
class VisitController {

	private static final String FORM_VIEW = "owners/visitForm";

	private final OwnerRepository owners;

	private final PetTypeRepository petTypes;

	private final VisitRepository visits;

	private final Clock clock;

	VisitController(OwnerRepository owners, PetTypeRepository petTypes, VisitRepository visits, Clock clock) {
		this.owners = owners;
		this.petTypes = petTypes;
		this.visits = visits;
		this.clock = clock;
	}

	/**
	 * Trims the form's values before validation; an empty value stays {@code ""} so it
	 * fails {@code @NotBlank} and is redisplayed as typed.
	 */
	@InitBinder("visitForm")
	void trimVisitForm(WebDataBinder binder) {
		binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
	}

	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/new.html")
	String initCreationForm(@PathVariable Integer ownerId, @PathVariable Integer petId, Model model) {
		Owner owner = findOwner(ownerId);
		Pet pet = findPet(owner, petId);
		model.addAttribute("visitForm", VisitForm.on(LocalDate.now(this.clock)));
		return form(model, owner, pet);
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits")
	ModelAndView processCreationForm(@PathVariable Integer ownerId, @PathVariable Integer petId,
			@Valid @ModelAttribute("visitForm") VisitForm form, BindingResult result, Model model,
			RedirectAttributes flash) {
		Owner owner = findOwner(ownerId);
		Pet pet = findPet(owner, petId);
		if (!result.hasFieldErrors("date") && IsoDates.parse(form.date()).isEmpty()) {
			result.rejectValue("date", "format", "Date must be a date in the form yyyy-mm-dd.");
		}
		if (result.hasErrors()) {
			return new ModelAndView(form(model, owner, pet));
		}
		this.visits.insert(pet.id(), IsoDates.parse(form.date()).orElseThrow(), form.description());
		flash.addFlashAttribute("message", "Your visit has been booked");
		return seeOther(ownerId);
	}

	private Owner findOwner(Integer ownerId) {
		return this.owners.findById(ownerId)
			.orElseThrow(
					() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner " + ownerId + " does not exist"));
	}

	/**
	 * Finds the pet among the owner's own, so a pet of another owner is as unknown as one
	 * that does not exist.
	 */
	private static Pet findPet(Owner owner, Integer petId) {
		return owner.pets()
			.stream()
			.filter((pet) -> pet.id().equals(petId))
			.findFirst()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "pet " + petId + " does not exist"));
	}

	/**
	 * Fills in the context block, the form's target and the pet's previous visits.
	 */
	private String form(Model model, Owner owner, Pet pet) {
		String typeName = this.petTypes.findAll()
			.stream()
			.filter((type) -> type.id().equals(pet.type().getId()))
			.map(PetType::name)
			.findFirst()
			.orElse(null);
		List<VisitView> previous = VisitView.mostRecentFirst(this.visits.findByPetIds(List.of(pet.id())));
		model.addAttribute("ownerName", owner.firstName() + " " + owner.lastName());
		model.addAttribute("petName", pet.name());
		model.addAttribute("petType", typeName);
		model.addAttribute("visits", previous);
		model.addAttribute("action", "/owners/" + owner.id() + "/pets/" + pet.id() + "/visits");
		return FORM_VIEW;
	}

	/**
	 * Post/redirect/get to the owner's details page with 303 See Other, which tells the
	 * browser to follow with a GET.
	 */
	private static ModelAndView seeOther(Integer ownerId) {
		RedirectView redirect = new RedirectView("/owners/" + ownerId + ".html", true);
		redirect.setStatusCode(HttpStatus.SEE_OTHER);
		redirect.setExposeModelAttributes(false);
		return new ModelAndView(redirect);
	}

}
