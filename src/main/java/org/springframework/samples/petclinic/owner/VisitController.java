package org.springframework.samples.petclinic.owner;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/new.html")
	String initCreationForm(@PathVariable Integer ownerId, @PathVariable Integer petId, Model model) {
		Owner owner = OwnerPages.findOwner(this.owners, ownerId);
		Pet pet = OwnerPages.findPet(owner, petId);
		model.addAttribute("visitForm", VisitForm.on(LocalDate.now(this.clock)));
		return form(model, owner, pet);
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits")
	ModelAndView processCreationForm(@PathVariable Integer ownerId, @PathVariable Integer petId,
			@Valid @ModelAttribute("visitForm") VisitForm form, BindingResult result, Model model,
			RedirectAttributes flash) {
		Owner owner = OwnerPages.findOwner(this.owners, ownerId);
		Pet pet = OwnerPages.findPet(owner, petId);
		if (!result.hasFieldErrors("date") && IsoDates.parse(form.date()).isEmpty()) {
			result.rejectValue("date", "format", "Date must be a date in the form yyyy-mm-dd.");
		}
		if (result.hasErrors()) {
			return new ModelAndView(form(model, owner, pet));
		}
		this.visits.insert(pet.id(), IsoDates.parse(form.date()).orElseThrow(), form.description());
		flash.addFlashAttribute("message", "Your visit has been booked");
		return OwnerPages.seeOther(ownerId);
	}

	/**
	 * Fills in the context block, the form's target and the pet's previous visits.
	 */
	private String form(Model model, Owner owner, Pet pet) {
		String typeName = this.petTypes.findById(pet.type().getId()).map(PetType::name).orElse(null);
		List<VisitView> previous = VisitView.mostRecentFirst(this.visits.findByPetIds(List.of(pet.id())));
		model.addAttribute("ownerName", owner.firstName() + " " + owner.lastName());
		model.addAttribute("petName", pet.name());
		model.addAttribute("petType", typeName);
		model.addAttribute("visits", previous);
		model.addAttribute("action", "/owners/" + owner.id() + "/pets/" + pet.id() + "/visits");
		return FORM_VIEW;
	}

}
