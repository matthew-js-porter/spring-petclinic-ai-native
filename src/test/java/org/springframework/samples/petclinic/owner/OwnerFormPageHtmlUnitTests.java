package org.springframework.samples.petclinic.owner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the New Owner and Edit Owner forms like a browser through MockMvc's HtmlUnit
 * integration, against the seeded database. Requests run on the test thread, so each
 * test's writes roll back and other tests still see the eleven seed owners.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnerFormPageHtmlUnitTests {

	private static final List<String> FIELDS = List.of("firstName", "lastName", "address", "city", "telephone");

	@Autowired
	private WebClient webClient;

	@Test
	void showsABlankLabelledForm() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/new.html");

		assertThat(page.getTitleText()).isEqualTo("New Owner · Spring PetClinic");
		assertThat(page.querySelectorAll("h2")).singleElement()
			.satisfies((h2) -> assertThat(h2.asNormalizedText()).isEqualTo("New Owner"));
		HtmlForm form = form(page);
		assertThat(form.getMethodAttribute()).isEqualTo("post");
		assertThat(form.getActionAttribute()).isEqualTo("/owners");
		assertThat(form.hasAttribute("novalidate")).isTrue();
		assertThat(texts(page, "form label")).containsExactly("First Name", "Last Name", "Address", "City",
				"Telephone");
		assertThat(inputs(page)).extracting(HtmlInput::getId).containsExactlyElementsOf(FIELDS);
		assertThat(inputs(page)).extracting((input) -> input.getAttribute("autocomplete"))
			.containsExactly("given-name", "family-name", "street-address", "address-level2", "tel-national");
		assertThat(inputs(page)).allSatisfy((input) -> {
			assertThat(input.getValue()).isEmpty();
			assertThat(input.getAttribute("aria-required")).isEqualTo("true");
			assertThat(input.hasAttribute("aria-invalid")).isFalse();
			assertThat(page.<DomElement>querySelector("label[for='" + input.getId() + "']")).isNotNull();
		});
		assertThat(page.<HtmlInput>querySelector("#telephone").getTypeAttribute()).isEqualTo("tel");
		assertThat(form.querySelectorAll("button")).singleElement()
			.satisfies((button) -> assertThat(button.asNormalizedText()).isEqualTo("Add Owner"));
		assertThat(page.querySelectorAll("[role=alert], .pc-field-error")).isEmpty();
		assertThat(currentNavItems(page)).containsExactly("/owners.html");
	}

	@Test
	void createsAnOwnerAndConfirmsOnce() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/new.html");
		fill(page, Map.of("firstName", "Ada", "lastName", "Lovelace", "address", "12 St James's Sq.", "city",
				"London", "telephone", "6085550000"));

		HtmlPage details = submit(page);

		assertThat(details.getTitleText()).isEqualTo("Owner Information · Spring PetClinic");
		int id = Integer.parseInt(details.getUrl().getPath().replaceAll("\\D", ""));
		assertThat(id).isGreaterThan(11);
		assertThat(texts(details, "dl.pc-detail dd")).containsExactly("Ada Lovelace", "12 St James's Sq.", "London",
				"6085550000");
		assertThat(texts(details, "[role=status]")).containsExactly("New Owner Created");
		assertThat(texts(details, ".pc-empty")).containsExactly("This owner has no pets.");

		HtmlPage reloaded = (HtmlPage) details.refresh();
		assertThat(reloaded.querySelectorAll("[role=status]")).isEmpty();

		HtmlPage results = this.webClient.getPage("/owners.html?lastName=love");
		assertThat(results.getAnchorByText("Ada Lovelace").getHrefAttribute()).isEqualTo("/owners/" + id + ".html");
	}

	@Test
	void editsAnOwnerAndKeepsTheirPets() throws IOException {
		HtmlPage details = this.webClient.getPage("/owners/1.html");
		HtmlPage page = details.getAnchorByText("Edit Owner").click();

		assertThat(page.getTitleText()).isEqualTo("Edit Owner · Spring PetClinic");
		assertThat(form(page).getActionAttribute()).isEqualTo("/owners/1");
		assertThat(inputs(page)).extracting(HtmlInput::getValue)
			.containsExactly("George", "Franklin", "110 W. Liberty St.", "Madison", "6085551023");
		assertThat(form(page).querySelector("button").asNormalizedText()).isEqualTo("Update Owner");
		assertThat(currentNavItems(page)).containsExactly("/owners.html");

		fill(page, Map.of("city", "Sun Prairie", "telephone", "6085559876"));
		HtmlPage updated = submit(page);

		assertThat(updated.getUrl().getPath()).isEqualTo("/owners/1.html");
		assertThat(texts(updated, "dl.pc-detail dd")).containsExactly("George Franklin", "110 W. Liberty St.",
				"Sun Prairie", "6085559876");
		assertThat(texts(updated, "[role=status]")).containsExactly("Owner Values Updated");
		assertThat(texts(updated, "#pets tbody td:first-child")).containsExactly("Leo");
	}

	@Test
	void explainsEveryProblemWithAnEmptyForm() throws IOException {
		HtmlPage page = submit(this.webClient.getPage("/owners/new.html"));

		assertThat(page.getTitleText()).isEqualTo("New Owner · Spring PetClinic");
		assertThat(texts(page, "[role=alert]")).containsExactly("Owner not saved: 5 fields need attention.");
		assertThat(inputs(page)).allSatisfy((input) -> {
			assertThat(input.getAttribute("aria-invalid")).isEqualTo("true");
			String describedBy = input.getAttribute("aria-describedby");
			assertThat(describedBy).isEqualTo(input.getId() + "-error");
			assertThat(page.getElementById(describedBy).asNormalizedText()).endsWith("is required.");
		});
		assertThat(texts(page, ".pc-field-error")).containsExactly("First Name is required.",
				"Last Name is required.", "Address is required.", "City is required.", "Telephone is required.");
		assertThat(currentNavItems(page)).containsExactly("/owners.html");
	}

	@Test
	void keepsWhatWasTypedAndFlagsOnlyTheBadTelephone() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/1/edit.html");
		fill(page, Map.of("firstName", "  Georgia ", "telephone", "6085551 23"));

		HtmlPage redisplay = submit(page);

		assertThat(texts(redisplay, "[role=alert]")).containsExactly("Owner not saved: 1 field needs attention.");
		assertThat(texts(redisplay, ".pc-field-error")).containsExactly("Telephone must be exactly 10 digits.");
		assertThat(redisplay.querySelectorAll(".pc-field-invalid input")).singleElement()
			.satisfies((input) -> assertThat(((DomElement) input).getId()).isEqualTo("telephone"));
		assertThat(inputs(redisplay)).extracting(HtmlInput::getValue)
			.containsExactly("Georgia", "Franklin", "110 W. Liberty St.", "Madison", "6085551 23");
		assertThat(form(redisplay).getActionAttribute()).isEqualTo("/owners/1");

		HtmlPage unchanged = this.webClient.getPage("/owners/1.html");
		assertThat(texts(unchanged, "dl.pc-detail dd")).first().isEqualTo("George Franklin");
	}

	@Test
	void showsMarkupInTheFieldsAsText() throws IOException {
		String script = "<script>alert(1)</script>";
		HtmlPage page = this.webClient.getPage("/owners/new.html");
		fill(page, Map.of("firstName", script, "lastName", "O'Neil & \"Sons\"", "address", script, "city", script,
				"telephone", script));

		HtmlPage redisplay = submit(page);
		assertThat(inputs(redisplay)).extracting(HtmlInput::getValue).contains(script, "O'Neil & \"Sons\"");
		assertThat(redisplay.querySelectorAll("main script")).isEmpty();

		fill(redisplay, Map.of("telephone", "6085550000"));
		HtmlPage details = submit(redisplay);
		assertThat(texts(details, "dl.pc-detail dd")).containsExactly(script + " O'Neil & \"Sons\"", script, script,
				"6085550000");
		assertThat(details.querySelectorAll("main script")).isEmpty();
	}

	@Test
	void usesNoButtonStyledLinksInlineStylesOrScripts() throws IOException {
		for (String path : List.of("/owners/new.html", "/owners/1/edit.html")) {
			HtmlPage page = this.webClient.getPage(path);
			assertThat(page.querySelectorAll("main a.pc-button, main [style], main script")).isEmpty();
		}
	}

	@Test
	void returnsNotFoundForTheEditFormOfAnUnknownOwner() throws IOException {
		this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

		assertThat(this.webClient.getPage("/owners/999/edit.html").getWebResponse().getStatusCode()).isEqualTo(404);
	}

	private static HtmlForm form(HtmlPage page) {
		return page.querySelector("main form");
	}

	private static List<HtmlInput> inputs(HtmlPage page) {
		return page.querySelectorAll("main form input").stream().map(HtmlInput.class::cast).toList();
	}

	private static void fill(HtmlPage page, Map<String, String> values) {
		values.forEach((field, value) -> page.<HtmlInput>querySelector("#" + field).setValue(value));
	}

	private static HtmlPage submit(HtmlPage page) throws IOException {
		return form(page).<HtmlButton>querySelector("button[type=submit]").click();
	}

	private static List<String> currentNavItems(HtmlPage page) {
		return page.querySelectorAll("nav[aria-label='Main'] [aria-current=page]")
			.stream()
			.map((link) -> ((HtmlAnchor) link).getHrefAttribute())
			.toList();
	}

	private static List<String> texts(HtmlPage page, String selector) {
		return page.querySelectorAll(selector).stream().map(DomNode::asNormalizedText).toList();
	}

}
