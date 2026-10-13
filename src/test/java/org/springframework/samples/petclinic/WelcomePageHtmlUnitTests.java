package org.springframework.samples.petclinic;

import java.io.IOException;
import java.util.List;

import org.htmlunit.Page;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlImage;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the welcome page like a browser through MockMvc's HtmlUnit integration.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WelcomePageHtmlUnitTests {

	@Autowired
	private WebClient webClient;

	@Test
	void showsTheWelcomeHeadingAndPhoto() throws IOException {
		HtmlPage page = this.webClient.getPage("/");

		assertThat(page.getTitleText()).isEqualTo("Welcome · Spring PetClinic");
		assertThat(page.querySelectorAll("h2")).hasSize(1);
		assertThat(page.querySelectorAll("main h2.pc-page-title")).singleElement()
			.extracting(DomNode::getTextContent)
			.isEqualTo("Welcome");

		assertThat(page.querySelectorAll("main img")).hasSize(1);
		HtmlImage image = page.querySelector("main img");
		assertThat(image.getSrcAttribute()).endsWith("/resources/images/pets.png");
		assertThat(image.getAltAttribute()).isNotBlank();

		Page photo = this.webClient.getPage(page.getFullyQualifiedUrl(image.getSrcAttribute()));
		assertThat(photo.getWebResponse().getStatusCode()).isEqualTo(200);
		assertThat(photo.getWebResponse().getContentType()).isEqualTo("image/png");
	}

	@Test
	void marksHomeAsTheCurrentNavItem() throws IOException {
		HtmlPage page = this.webClient.getPage("/");

		List<String> items = page.querySelectorAll("nav[aria-label='Main'] .pc-nav a")
			.stream()
			.map(DomNode::asNormalizedText)
			.toList();
		assertThat(items).containsExactly("Home", "Find Owners", "Veterinarians");

		assertThat(page.querySelectorAll("nav[aria-label='Main'] [aria-current]")).singleElement()
			.satisfies((current) -> {
				assertThat(((HtmlAnchor) current).getHrefAttribute()).isEqualTo("/");
				assertThat(((HtmlAnchor) current).getAttribute("aria-current")).isEqualTo("page");
			});
		HtmlAnchor brand = page.querySelector("nav[aria-label='Main'] a.pc-brand");
		assertThat(brand.getHrefAttribute()).isEqualTo("/");
	}

}
