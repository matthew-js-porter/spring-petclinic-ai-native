package org.springframework.samples.petclinic;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the welcome page at the root URL, which the navbar brand and Home item link to.
 */
@Controller
class WelcomeController {

	@GetMapping("/")
	String welcome() {
		return "welcome";
	}

}
