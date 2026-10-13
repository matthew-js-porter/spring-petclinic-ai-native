package org.springframework.samples.petclinic.owner;

import java.io.IOException;
import java.util.List;

import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlOption;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSelect;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the New Pet and Edit Pet forms like a browser through MockMvc's HtmlUnit
 * integration, against the seeded database. Requests run on the test thread, so each
 * test's writes roll back and other tests still see the thirteen seed pets.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PetFormPageHtmlUnitTests {

	@Autowired
	private WebClient webClient;

	@Test
	void showsABlankLabelledForm() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/1/pets/new.html");

		assertThat(page.getTitleText()).isEqualTo("New Pet · Spring PetClinic");
		assertThat(page.querySelectorAll("h2")).singleElement()
			.satisfies((h2) -> assertThat(h2.asNormalizedText()).isEqualTo("New Pet"));
		assertThat(texts(page, "main p.pc-muted")).containsExactly("Owner: George Franklin");
		HtmlForm form = form(page);
		assertThat(form.getMethodAttribute()).isEqualTo("post");
		assertThat(form.getActionAttribute()).isEqualTo("/owners/1/pets");
		assertThat(form.hasAttribute("novalidate")).isTrue();
		assertThat(texts(page, "form label")).containsExactly("Name", "Birth Date", "Type");
		assertThat(controls(page)).extracting(DomElement::getId).containsExactly("name", "birthDate", "typeId");
		assertThat(controls(page)).allSatisfy((control) -> {
			assertThat(control.getAttribute("aria-required")).isEqualTo("true");
			assertThat(control.hasAttribute("aria-invalid")).isFalse();
			assertThat(page.<DomElement>querySelector("label[for='" + control.getId() + "']")).isNotNull();
		});
		assertThat(name(page).getValue()).isEmpty();
		assertThat(name(page).getAttribute("autocomplete")).isEqualTo("off");
		assertThat(birthDate(page).getTypeAttribute()).isEqualTo("date");
		assertThat(birthDate(page).getValue()).isEmpty();
		assertThat(birthDate(page).getAttribute("aria-describedby")).isEqualTo("birthDate-help");
		assertThat(page.getElementById("birthDate-help").asNormalizedText()).isEqualTo("yyyy-mm-dd");
		assertThat(type(page).getOptions()).extracting(HtmlOption::getText)
			.containsExactly("Select a type", "bird", "cat", "dog", "hamster", "lizard", "snake");
		assertThat(type(page).getSelectedOptions()).extracting(HtmlOption::getValueAttribute).containsExactly("");
		assertThat(form.querySelectorAll("button")).singleElement()
			.satisfies((button) -> assertThat(button.asNormalizedText()).isEqualTo("Add Pet"));
		assertThat(page.querySelectorAll("[role=alert], .pc-field-error, main [style], main script")).isEmpty();
		assertThat(currentNavItems(page)).containsExactly("/owners.html");
	}

	@Test
	void addsAPetAndConfirmsOnce() throws IOException {
		HtmlPage page = this.webClient.<HtmlPage>getPage("/owners/1.html").getAnchorByText("Add New Pet").click();
		fill(page, "Rex", "2023-04-12", "dog");

		HtmlPage details = submit(page);

		assertThat(details.getUrl().getPath()).isEqualTo("/owners/1.html");
		assertThat(texts(details, "[role=status]")).containsExactly("New Pet has been Added");
		assertThat(texts(details, "#pets tbody td:first-child")).containsExactly("Leo", "Rex");
		assertThat(texts(details, "#pets tbody tr:last-child td")).containsExactly("Rex", "2023-04-12", "dog",
				"Edit Pet Rex" + "Add Visit Rex");
		assertThat(((HtmlPage) details.refresh()).querySelectorAll("[role=status]")).isEmpty();

		String json = this.webClient.getPage("/owners/1").getWebResponse().getContentAsString();
		assertThat(json).contains("\"name\":\"Rex\",\"birthDate\":\"2023-04-12\",\"type\":\"dog\"");
		assertThat(this.webClient.getPage("/owners.html?lastName=Franklin").getWebResponse().getContentAsString())
			.contains("Leo Rex");
	}

	@Test
	void givesAnOwnerWithoutPetsTheirFirst() throws IOException {
		HtmlPage page = this.webClient.<HtmlPage>getPage("/owners/11.html").getAnchorByText("Add New Pet").click();
		assertThat(texts(page, "main p.pc-muted")).containsExactly("Owner: Helen Ostrander");
		fill(page, "Biscuit", "2024-05-01", "hamster");

		HtmlPage details = submit(page);

		assertThat(texts(details, ".pc-empty")).containsExactly("This pet has no visits.");
		assertThat(texts(details, "#pets tbody td:first-child")).containsExactly("Biscuit");
		int id = Integer.parseInt(details.<HtmlAnchor>querySelector("#pets .pc-col-actions a:first-child")
			.getHrefAttribute()
			.replaceAll("/owners/11/pets/(\\d+)/edit.html", "$1"));
		assertThat(id).isGreaterThan(13);
	}

	@Test
	void editsAPetAndKeepsTheOthers() throws IOException {
		HtmlPage details = this.webClient.getPage("/owners/6.html");
		HtmlPage page = details.getAnchorByText("Edit Pet Max").click();

		assertThat(page.getTitleText()).isEqualTo("Edit Pet · Spring PetClinic");
		assertThat(form(page).getActionAttribute()).isEqualTo("/owners/6/pets/8");
		assertThat(name(page).getValue()).isEqualTo("Max");
		assertThat(birthDate(page).getValue()).isEqualTo("2012-09-04");
		assertThat(type(page).getSelectedOptions()).extracting(HtmlOption::getText).containsExactly("cat");
		assertThat(form(page).querySelector("button").asNormalizedText()).isEqualTo("Update Pet");
		assertThat(currentNavItems(page)).containsExactly("/owners.html");

		fill(page, "Maximus", "2012-09-05", "dog");
		HtmlPage updated = submit(page);

		assertThat(updated.getUrl().getPath()).isEqualTo("/owners/6.html");
		assertThat(texts(updated, "[role=status]")).containsExactly("Pet details has been edited");
		assertThat(rows(updated)).containsExactly(
				List.of("Maximus", "2012-09-05", "dog", "Edit Pet Maximus" + "Add Visit Maximus"),
				List.of("Samantha", "2012-09-04", "cat", "Edit Pet Samantha" + "Add Visit Samantha"));
		assertThat(editLinks(updated)).containsExactly("/owners/6/pets/8/edit.html", "/owners/6/pets/7/edit.html");
	}

	@Test
	void rejectsADuplicateNameAndTiesTheMessageToTheField() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/1/pets/new.html");
		fill(page, "leo", "2023-04-12", "dog");

		HtmlPage redisplay = submit(page);

		assertThat(texts(redisplay, "[role=alert]")).containsExactly("Pet not saved: 1 field needs attention.");
		assertThat(texts(redisplay, ".pc-field-error")).containsExactly("This owner already has a pet with this name.");
		assertThat(name(redisplay).getValue()).isEqualTo("leo");
		assertThat(name(redisplay).getAttribute("aria-invalid")).isEqualTo("true");
		assertThat(redisplay.getElementById(name(redisplay).getAttribute("aria-describedby")).asNormalizedText())
			.isEqualTo("This owner already has a pet with this name.");
		assertThat(birthDate(redisplay).getValue()).isEqualTo("2023-04-12");
		assertThat(type(redisplay).getSelectedOptions()).extracting(HtmlOption::getText).containsExactly("dog");
		assertThat(currentNavItems(redisplay)).containsExactly("/owners.html");
		assertThat(texts(this.webClient.<HtmlPage>getPage("/owners/1.html"), "#pets tbody td:first-child"))
			.containsExactly("Leo");
	}

	@Test
	void explainsEveryProblemWithAnEmptyForm() throws IOException {
		HtmlPage page = submit(this.webClient.getPage("/owners/1/pets/new.html"));

		assertThat(page.getTitleText()).isEqualTo("New Pet · Spring PetClinic");
		assertThat(texts(page, "[role=alert]")).containsExactly("Pet not saved: 3 fields need attention.");
		assertThat(texts(page, ".pc-field-error")).containsExactly("Name is required.", "Birth Date is required.",
				"Type is required.");
		assertThat(controls(page)).allSatisfy((control) -> {
			assertThat(control.getAttribute("aria-invalid")).isEqualTo("true");
			List<String> ids = List.of(control.getAttribute("aria-describedby").split(" "));
			assertThat(ids).last().isEqualTo(control.getId() + "-error");
			assertThat(ids).allSatisfy((id) -> assertThat(page.getElementById(id)).isNotNull());
		});
		assertThat(birthDate(page).getAttribute("aria-describedby")).isEqualTo("birthDate-help birthDate-error");
		assertThat(currentNavItems(page)).containsExactly("/owners.html");
	}

	@Test
	void rejectsAFutureBirthDateAndKeepsTheOtherValues() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/1/pets/new.html");
		fill(page, "Rex", "2999-01-01", "bird");

		HtmlPage redisplay = submit(page);

		assertThat(texts(redisplay, ".pc-field-error")).containsExactly("Birth Date cannot be in the future.");
		assertThat(redisplay.querySelectorAll(".pc-field-invalid")).singleElement()
			.satisfies((field) -> assertThat(field.<DomElement>querySelector("input").getId()).isEqualTo("birthDate"));
		assertThat(name(redisplay).getValue()).isEqualTo("Rex");
		assertThat(birthDate(redisplay).getValue()).isEqualTo("2999-01-01");
		assertThat(type(redisplay).getSelectedOptions()).extracting(HtmlOption::getText).containsExactly("bird");
	}

	@Test
	void showsMarkupInTheNameAsText() throws IOException {
		String script = "<script>alert(1)</script>";
		HtmlPage page = this.webClient.getPage("/owners/1/pets/new.html");
		fill(page, script, "", "cat");

		HtmlPage redisplay = submit(page);
		assertThat(name(redisplay).getValue()).isEqualTo(script);
		assertThat(redisplay.querySelectorAll("main script")).isEmpty();

		birthDate(redisplay).setValue("2020-02-02");
		HtmlPage details = submit(redisplay);
		assertThat(texts(details, "#pets tbody td:first-child")).contains(script);
		assertThat(details.querySelectorAll("main script")).isEmpty();
		assertThat(this.webClient.getPage("/owners/1").getWebResponse().getContentAsString())
			.contains("\"name\":\"<script>alert(1)</script>\"");
	}

	@Test
	void returnsNotFoundForAnotherOwnersPetOrAnUnknownOwner() throws IOException {
		this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

		assertThat(this.webClient.getPage("/owners/1/pets/2/edit.html").getWebResponse().getStatusCode())
			.isEqualTo(404);
		assertThat(this.webClient.getPage("/owners/999/pets/new.html").getWebResponse().getStatusCode())
			.isEqualTo(404);
	}

	private static HtmlForm form(HtmlPage page) {
		return page.querySelector("main form");
	}

	private static List<HtmlElement> controls(HtmlPage page) {
		return page.querySelectorAll("main form input, main form select")
			.stream()
			.map(HtmlElement.class::cast)
			.toList();
	}

	private static HtmlInput name(HtmlPage page) {
		return page.querySelector("#name");
	}

	private static HtmlInput birthDate(HtmlPage page) {
		return page.querySelector("#birthDate");
	}

	private static HtmlSelect type(HtmlPage page) {
		return page.querySelector("#typeId");
	}

	private static void fill(HtmlPage page, String name, String birthDate, String type) {
		name(page).setValue(name);
		birthDate(page).setValue(birthDate);
		type(page).getOptionByText(type).setSelected(true);
	}

	private static HtmlPage submit(HtmlPage page) throws IOException {
		return form(page).<HtmlButton>querySelector("button[type=submit]").click();
	}

	private static List<List<String>> rows(HtmlPage page) {
		return page.querySelectorAll("#pets tbody tr")
			.stream()
			.map((row) -> row.querySelectorAll("td").stream().map(DomNode::asNormalizedText).toList())
			.toList();
	}

	private static List<String> editLinks(HtmlPage page) {
		return page.querySelectorAll("#pets .pc-col-actions a:first-child")
			.stream()
			.map((link) -> ((HtmlAnchor) link).getHrefAttribute())
			.toList();
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
