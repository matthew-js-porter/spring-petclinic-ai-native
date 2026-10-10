package org.springframework.samples.petclinic;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * There is no home page yet, so the root URL (and the navbar brand link) leads to the
 * vets list.
 */
@Controller
class RootController {

	@GetMapping("/")
	String root() {
		return "redirect:/vets.html";
	}

}
