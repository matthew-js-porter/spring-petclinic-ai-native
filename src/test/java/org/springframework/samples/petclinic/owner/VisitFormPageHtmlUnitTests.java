package org.springframework.samples.petclinic.owner;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlElement;
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
 * Drives the New Visit form like a browser through MockMvc's HtmlUnit integration,
 * against the seeded database. Requests run on the test thread, so each test's writes
 * roll back and other tests still see the four seed visits.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VisitFormPageHtmlUnitTests {

	@Autowired
	private WebClient webClient;

	@Test
	void showsTheContextALabelledFormAndThePreviousVisits() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/6/pets/7/visits/new.html");

		assertThat(page.getTitleText()).isEqualTo("New Visit · Spring PetClinic");
		assertThat(page.querySelectorAll("h2")).singleElement()
			.satisfies((h2) -> assertThat(h2.asNormalizedText()).isEqualTo("New Visit"));
		assertThat(texts(page, "dl.pc-context dt")).containsExactly("Pet", "Type", "Owner");
		assertThat(texts(page, "dl.pc-context dd")).containsExactly("Samantha", "cat", "Jean Coleman");
		HtmlForm form = form(page);
		assertThat(form.getMethodAttribute()).isEqualTo("post");
		assertThat(form.getActionAttribute()).isEqualTo("/owners/6/pets/7/visits");
		assertThat(form.hasAttribute("novalidate")).isTrue();
		assertThat(texts(page, "form label")).containsExactly("Date", "Description");
		assertThat(controls(page)).extracting(DomElement::getId).containsExactly("date", "description");
		assertThat(controls(page)).allSatisfy((control) -> {
			assertThat(control.hasAttribute("required")).isTrue();
			assertThat(control.hasAttribute("aria-invalid")).isFalse();
			assertThat(page.<DomElement>querySelector("label[for='" + control.getId() + "']")).isNotNull();
		});
		assertThat(date(page).getTypeAttribute()).isEqualTo("date");
		assertThat(date(page).getValue()).isEqualTo(LocalDate.now().toString());
		assertThat(date(page).hasAttribute("max")).isFalse();
		assertThat(description(page).getTypeAttribute()).isEqualTo("text");
		assertThat(description(page).getValue()).isEmpty();
		assertThat(description(page).getAttribute("maxlength")).isEqualTo("255");
		assertThat(form.querySelectorAll("button")).singleElement()
			.satisfies((button) -> assertThat(button.asNormalizedText()).isEqualTo("Add Visit"));
		assertThat(texts(page, "h3")).containsExactly("Previous Visits");
		assertThat(texts(page, "table.pc-visits caption")).containsExactly("Visits of Samantha");
		assertThat(page.querySelectorAll("table.pc-visits th"))
			.allSatisfy((th) -> assertThat(((DomElement) th).getAttribute("scope")).isEqualTo("col"));
		assertThat(texts(page, "table.pc-visits tbody tr")).containsExactly("2013-01-04\tspayed",
				"2013-01-01\trabies shot");
		assertThat(page.querySelectorAll("[role=alert], .pc-field-error, main [style], main script")).isEmpty();
		assertThat(currentNavItems(page)).containsExactly("/owners.html");
	}

	@Test
	void explainsWhenThePetHasNoVisits() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/1/pets/1/visits/new.html");

		assertThat(texts(page, "dl.pc-context dd")).containsExactly("Leo", "cat", "George Franklin");
		assertThat(texts(page, "h3 + p.pc-empty")).containsExactly("This pet has no visits.");
		assertThat(page.querySelectorAll("table")).isEmpty();
	}

	@Test
	void explainsEveryProblemWithAnEmptyForm() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/6/pets/7/visits/new.html");
		date(page).setValue("");

		HtmlPage redisplay = submit(page);

		assertThat(redisplay.getTitleText()).isEqualTo("New Visit · Spring PetClinic");
		assertThat(texts(redisplay, "[role=alert]")).containsExactly("Visit not saved: 2 fields need attention.");
		assertThat(texts(redisplay, ".pc-field-error")).containsExactly("Date is required.",
				"Description is required.");
		assertThat(controls(redisplay)).allSatisfy((control) -> {
			assertThat(control.getAttribute("aria-invalid")).isEqualTo("true");
			List<String> ids = List.of(control.getAttribute("aria-describedby").split(" "));
			assertThat(ids).last().isEqualTo(control.getId() + "-error");
			assertThat(ids).allSatisfy((id) -> assertThat(redisplay.getElementById(id)).isNotNull());
		});
		assertThat(texts(redisplay, "table.pc-visits tbody tr")).hasSize(2);
		assertThat(currentNavItems(redisplay)).containsExactly("/owners.html");
	}

	@Test
	void rejectsABadDateAndKeepsTheDescription() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/6/pets/7/visits/new.html");
		date(page).setValue("2024-02-30");
		description(page).setValue("checkup");

		HtmlPage redisplay = submit(page);

		assertThat(texts(redisplay, "[role=alert]")).containsExactly("Visit not saved: 1 field needs attention.");
		assertThat(texts(redisplay, ".pc-field-error")).containsExactly("Date must be a date in the form yyyy-mm-dd.");
		assertThat(description(redisplay).getValue()).isEqualTo("checkup");
		assertThat(this.webClient.getPage("/owners/6").getWebResponse().getContentAsString()).doesNotContain("checkup");
	}

	@Test
	void booksAVisitFromTheOwnerPageAndConfirmsOnce() throws IOException {
		HtmlPage details = this.webClient.getPage("/owners/6.html");
		HtmlPage page = details.getAnchorByText("Add Visit Max").click();
		assertThat(page.getUrl().getPath()).isEqualTo("/owners/6/pets/8/visits/new.html");
		date(page).setValue("2013-01-05");
		description(page).setValue("dental cleaning");

		HtmlPage booked = submit(page);

		assertThat(booked.getUrl().getPath()).isEqualTo("/owners/6.html");
		assertThat(texts(booked, "[role=status]")).containsExactly("Your visit has been booked");
		assertThat(texts(booked, "table.pc-visits tbody tr")).containsExactly("2013-01-05\tdental cleaning",
				"2013-01-03\tneutered", "2013-01-02\trabies shot", "2013-01-04\tspayed", "2013-01-01\trabies shot");
		assertThat(((HtmlPage) booked.refresh()).querySelectorAll("[role=status]")).isEmpty();
		assertThat(this.webClient.getPage("/owners/6").getWebResponse().getContentAsString())
			.contains("\"date\":\"2013-01-05\",\"description\":\"dental cleaning\"");
	}

	@Test
	void replacesTheEmptySentenceWithTheFirstVisit() throws IOException {
		HtmlPage page = this.webClient.<HtmlPage>getPage("/owners/1.html").getAnchorByText("Add Visit Leo").click();
		description(page).setValue("first checkup");

		HtmlPage booked = submit(page);

		assertThat(texts(booked, "p.pc-empty")).isEmpty();
		assertThat(texts(booked, "table.pc-visits tbody tr"))
			.containsExactly(LocalDate.now() + "\tfirst checkup");
	}

	@Test
	void showsMarkupInTheDescriptionAsText() throws IOException {
		String script = "<script>alert(1)</script> & \"x\" 'y'";
		HtmlPage page = this.webClient.getPage("/owners/1/pets/1/visits/new.html");
		date(page).setValue("");
		description(page).setValue(script);

		HtmlPage redisplay = submit(page);
		assertThat(description(redisplay).getValue()).isEqualTo(script);
		assertThat(redisplay.querySelectorAll("main script")).isEmpty();

		date(redisplay).setValue("2020-02-02");
		HtmlPage details = submit(redisplay);
		assertThat(texts(details, "table.pc-visits td:last-child")).containsExactly(script);
		assertThat(details.querySelectorAll("main script")).isEmpty();
	}

	@Test
	void returnsNotFoundForAnotherOwnersPetOrAnUnknownOwner() throws IOException {
		this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

		assertThat(this.webClient.getPage("/owners/1/pets/7/visits/new.html").getWebResponse().getStatusCode())
			.isEqualTo(404);
		assertThat(this.webClient.getPage("/owners/999/pets/1/visits/new.html").getWebResponse().getStatusCode())
			.isEqualTo(404);
	}

	private static HtmlForm form(HtmlPage page) {
		return page.querySelector("main form");
	}

	private static List<HtmlElement> controls(HtmlPage page) {
		return page.querySelectorAll("main form input").stream().map(HtmlElement.class::cast).toList();
	}

	private static HtmlInput date(HtmlPage page) {
		return page.querySelector("#date");
	}

	private static HtmlInput description(HtmlPage page) {
		return page.querySelector("#description");
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
