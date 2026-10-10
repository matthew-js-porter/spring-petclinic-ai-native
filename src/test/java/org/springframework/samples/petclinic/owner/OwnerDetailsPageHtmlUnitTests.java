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

		assertThat(texts(page, "h3")).containsExactly("Pets");
		assertThat(texts(page, "#pets caption")).containsExactly("Pets of George Franklin");
		assertThat(texts(page, "#pets thead th")).containsExactly("Name", "Birth Date", "Type");
		assertThat(page.querySelectorAll("#pets thead th"))
			.allSatisfy((th) -> assertThat(((DomElement) th).getAttribute("scope")).isEqualTo("col"));
		assertThat(pets(page)).containsExactly(List.of("Leo", "2010-09-07", "cat"));
	}

	@Test
	void sortsPetsByName() throws IOException {
		assertThat(pets(this.webClient.getPage("/owners/6.html"))).containsExactly(
				List.of("Max", "2012-09-04", "cat"), List.of("Samantha", "2012-09-04", "cat"));
		assertThat(pets(this.webClient.getPage("/owners/3.html"))).containsExactly(
				List.of("Jewel", "2010-03-07", "dog"), List.of("Rosy", "2011-04-17", "dog"));
	}

	@Test
	void explainsWhenTheOwnerHasNoPets() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/11.html");

		assertThat(texts(page, "dl.pc-detail dd")).containsExactly("Helen Ostrander", "4120 Monroe St.", "Fitchburg",
				"6085554412");
		assertThat(texts(page, "h3")).containsExactly("Pets");
		assertThat(texts(page, "p.pc-muted")).containsExactly("This owner has no pets.");
		assertThat(page.querySelectorAll("table")).isEmpty();
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
	void offersNoEditAddOrVisitActions() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners/1.html");

		assertThat(page.querySelectorAll("main button, main form")).isEmpty();
		assertThat(page.querySelectorAll("main a")).isEmpty();
	}

	@Test
	void returnsNotFoundForAnUnknownOwner() throws IOException {
		this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

		assertThat(this.webClient.getPage("/owners/999.html").getWebResponse().getStatusCode()).isEqualTo(404);
	}

	private static List<String> texts(HtmlPage page, String selector) {
		return page.querySelectorAll(selector).stream().map(DomNode::asNormalizedText).toList();
	}

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
