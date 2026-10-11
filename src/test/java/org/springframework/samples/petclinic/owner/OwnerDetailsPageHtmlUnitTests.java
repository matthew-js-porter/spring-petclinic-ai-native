package org.springframework.samples.petclinic.owner;

import java.io.IOException;
import java.util.List;

import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTable;
import org.htmlunit.html.HtmlTableRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the owner details page like a browser through MockMvc's HtmlUnit integration,
 * against the seeded database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OwnerDetailsPageHtmlUnitTests {

	@Autowired
	private WebClient webClient;

	@BeforeEach
	void disableJavaScript() {
		// HtmlUnit's JavaScript engine cannot parse Bootstrap 5.3's bundle, which the page
		// only uses for the collapsed navbar; that is checked in a real browser instead.
		this.webClient.getOptions().setJavaScriptEnabled(false);
	}

	@Test
	void showsTheOwnerInformation() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/1.html");

		assertThat(page.getTitleText()).isEqualTo("Owner Information · Spring PetClinic");
		assertThat(page.querySelectorAll("h2")).singleElement()
			.satisfies((h2) -> assertThat(h2.asNormalizedText()).isEqualTo("Owner Information"));
		assertThat(texts(page, "dl.pc-detail dt")).containsExactly("Name", "Address", "City", "Telephone");
		assertThat(texts(page, "dl.pc-detail dd")).containsExactly("George Franklin", "110 W. Liberty St.", "Madison",
				"6085551023");
	}

	@Test
	void listsThePetsWithBirthDateAndType() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/1.html");

		assertThat(texts(page, "h3")).containsExactly("Pets", "Visits");
		assertThat(texts(page, "#pets caption")).containsExactly("Pets of George Franklin");
		assertThat(texts(page, "#pets thead th")).containsExactly("Name", "Birth Date", "Type", "Actions");
		assertThat(page.querySelectorAll("#pets thead th"))
			.allSatisfy((th) -> assertThat(((DomElement) th).getAttribute("scope")).isEqualTo("col"));
		assertThat(pets(page)).containsExactly(List.of("Leo", "2010-09-07", "cat", "Edit Pet Leo" + "Add Visit Leo"));
		assertThat(page.querySelectorAll(".pc-table-region[role=region][tabindex='0'] > #pets")).hasSize(1);
	}

	@Test
	void sortsPetsByName() throws IOException {
		assertThat(pets(this.webClient.getPage("/owners/6.html"))).containsExactly(
				List.of("Max", "2012-09-04", "cat", "Edit Pet Max" + "Add Visit Max"),
				List.of("Samantha", "2012-09-04", "cat", "Edit Pet Samantha" + "Add Visit Samantha"));
		assertThat(pets(this.webClient.getPage("/owners/3.html"))).containsExactly(
				List.of("Jewel", "2010-03-07", "dog", "Edit Pet Jewel" + "Add Visit Jewel"),
				List.of("Rosy", "2011-04-17", "dog", "Edit Pet Rosy" + "Add Visit Rosy"));
	}

	@Test
	void explainsWhenTheOwnerHasNoPets() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/11.html");

		assertThat(texts(page, "dl.pc-detail dd")).containsExactly("Helen Ostrander", "4120 Monroe St.", "Fitchburg",
				"6085554412");
		assertThat(texts(page, "h3")).containsExactly("Pets");
		assertThat(texts(page, "p.pc-muted")).containsExactly("This owner has no pets.");
		assertThat(page.querySelectorAll("table")).isEmpty();
		assertThat(page.<HtmlAnchor>querySelector("h3 + a.pc-action")).satisfies((add) -> {
			assertThat(add.asNormalizedText()).isEqualTo("Add New Pet");
			assertThat(add.getHrefAttribute()).isEqualTo("/owners/11/pets/new.html");
		});
	}

	@Test
	void marksFindOwnersAsTheOnlyCurrentNavItem() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/1.html");

		assertThat(page.querySelectorAll("nav[aria-label='Main'] [aria-current]")).singleElement()
			.satisfies((current) -> {
				assertThat(((HtmlAnchor) current).getHrefAttribute()).isEqualTo("/owners.html");
				assertThat(((HtmlAnchor) current).getAttribute("aria-current")).isEqualTo("page");
			});
	}

	@Test
	void offersEditOwnerAddNewPetEditPetAndAddVisitOnly() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/6.html");

		assertThat(page.querySelectorAll("main button, main form")).isEmpty();
		assertThat(page.querySelectorAll("main a")).extracting((link) -> ((HtmlAnchor) link).getHrefAttribute())
			.containsExactly("/owners/6/edit.html", "/owners/6/pets/new.html", "/owners/6/pets/8/edit.html",
					"/owners/6/pets/8/visits/new.html", "/owners/6/pets/7/edit.html", "/owners/6/pets/7/visits/new.html");
		assertThat(texts(page, "main a")).containsExactly("Edit Owner", "Add New Pet", "Edit Pet Max", "Add Visit Max",
				"Edit Pet Samantha", "Add Visit Samantha");
		assertThat(page.<HtmlAnchor>querySelector("h3 + a.pc-action").asNormalizedText()).isEqualTo("Add New Pet");
		assertThat(texts(page, "#pets td.pc-col-actions .visually-hidden")).containsExactly("Max", "Max", "Samantha",
				"Samantha");
		assertThat(page.querySelectorAll("[role=status]")).isEmpty();
	}

	@Test
	void listsEachPetsVisitsMostRecentFirst() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/6.html");

		assertThat(texts(page, "h3")).containsExactly("Pets", "Visits");
		assertThat(texts(page, "h4.pc-subsection-title")).containsExactly("Max", "Samantha");
		assertThat(texts(page, "table.pc-visits caption")).containsExactly("Visits of Max", "Visits of Samantha");
		assertThat(texts(page, "table.pc-visits thead th")).containsExactly("Date", "Description", "Date",
				"Description");
		assertThat(page.querySelectorAll("table.pc-visits th"))
			.allSatisfy((th) -> assertThat(((DomElement) th).getAttribute("scope")).isEqualTo("col"));
		assertThat(texts(page, "table.pc-visits tbody tr")).containsExactly("2013-01-03\tneutered",
				"2013-01-02\trabies shot", "2013-01-04\tspayed", "2013-01-01\trabies shot");
		assertThat(page.querySelectorAll("table table")).isEmpty();
	}

	@Test
	void explainsWhenAPetHasNoVisits() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/3.html");

		assertThat(texts(page, "h4.pc-subsection-title")).containsExactly("Jewel", "Rosy");
		assertThat(texts(page, "h4 + p.pc-empty")).containsExactly("This pet has no visits.", "This pet has no visits.");
		assertThat(page.querySelectorAll("table.pc-visits")).isEmpty();
	}

	@Test
	void returnsNotFoundForAnUnknownOwner() throws IOException {
		this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

		assertThat(this.webClient.getPage("/owners/999.html").getWebResponse().getStatusCode()).isEqualTo(404);
	}

	private static List<String> texts(HtmlPage page, String selector) {
		return page.querySelectorAll(selector).stream().map(DomNode::asNormalizedText).toList();
	}

	/**
	 * Each row's cell texts. The actions cell's two links sit side by side with no space
	 * between them in the markup, so their texts run together.
	 */
	private static List<List<String>> pets(HtmlPage page) {
		HtmlTable table = page.getHtmlElementById("pets");
		return table.getBodies()
			.getFirst()
			.getRows()
			.stream()
			.map(HtmlTableRow::getCells)
			.map((cells) -> cells.stream().map(DomNode::asNormalizedText).toList())
			.toList();
	}

}
