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
 * Drives the owners page like a browser through MockMvc's HtmlUnit integration, against
 * the seeded database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OwnersPageHtmlUnitTests {

	@Autowired
	private WebClient webClient;

	@BeforeEach
	void disableJavaScript() {
		// HtmlUnit's JavaScript engine cannot parse Bootstrap 5.3's bundle, which the page
		// only uses for the collapsed navbar; that is checked in a real browser instead.
		this.webClient.getOptions().setJavaScriptEnabled(false);
	}

	@Test
	void showsTheFirstPageOfOwners() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners.html");

		assertThat(page.getTitleText()).isEqualTo("Owners · Spring PetClinic");
		assertThat(page.querySelectorAll("h2")).singleElement()
			.satisfies((h2) -> assertThat(h2.asNormalizedText()).isEqualTo("Owners"));
		assertThat(page.querySelectorAll("#owners thead th").stream().map(DomNode::asNormalizedText).toList())
			.containsExactly("Name", "Address", "City", "Telephone", "Pets");
		assertThat(rows(page)).hasSize(5)
			.first()
			.isEqualTo(List.of("George Franklin", "110 W. Liberty St.", "Madison", "6085551023", "Leo"));
		assertThat(rows(page)).extracting(List::getFirst)
			.containsExactly("George Franklin", "Betty Davis", "Eduardo Rodriquez", "Harold Davis", "Peter McTavish");
		assertThat(page.querySelectorAll("#owners tbody a")).isEmpty();
	}

	@Test
	void wrapsTheTableInAFocusableLabelledRegion() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners.html");

		DomElement region = page.querySelector("[role=region]");
		assertThat(region.getAttribute("aria-label")).isEqualTo("Owners table");
		assertThat(region.getAttribute("tabindex")).isEqualTo("0");
		assertThat(region.<DomElement>querySelector("#owners")).isNotNull();
	}

	@Test
	void movesToTheSecondPage() throws IOException {
		HtmlPage page1 = this.webClient.getPage("/owners.html");

		HtmlPage page2 = page1.getAnchorByText("2").click();

		assertThat(page2.getUrl().toString()).endsWith("/owners.html?page=2");
		assertThat(rows(page2)).extracting(List::getFirst)
			.containsExactly("Jean Coleman", "Jeff Black", "Maria Escobito", "David Schroeder", "Carlos Estaban");
		assertThat(rows(page2).getFirst()).last().isEqualTo("Max Samantha");
		assertThat(rows(page2).getLast()).last().isEqualTo("Lucky Sly");
		assertThat(page2.querySelectorAll("a[aria-label='Next page'], a[aria-label='Last page']")).isEmpty();
	}

	@Test
	void marksFindOwnersAsTheOnlyCurrentNavItem() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners.html");

		assertThat(navItems(page)).containsExactly("Home", "Find Owners", "Veterinarians");
		assertThat(page.querySelectorAll("nav[aria-label='Main'] [aria-current]")).singleElement()
			.satisfies((current) -> {
				assertThat(((HtmlAnchor) current).getHrefAttribute()).isEqualTo("/owners.html");
				assertThat(((HtmlAnchor) current).getAttribute("aria-current")).isEqualTo("page");
			});
	}

	@Test
	void doesNotMarkFindOwnersCurrentOnTheVetsPage() throws IOException {
		HtmlPage vets = this.webClient.getPage("/vets.html");

		HtmlAnchor findOwners = vets.querySelector("nav[aria-label='Main'] a[href='/owners.html']");
		assertThat(findOwners.hasAttribute("aria-current")).isFalse();

		HtmlPage owners = findOwners.click();
		assertThat(owners.getTitleText()).isEqualTo("Owners · Spring PetClinic");
	}

	private static List<String> navItems(HtmlPage page) {
		return page.querySelectorAll("nav[aria-label='Main'] .nav-item a")
			.stream()
			.map(DomNode::asNormalizedText)
			.toList();
	}

	private static List<List<String>> rows(HtmlPage page) {
		HtmlTable table = page.getHtmlElementById("owners");
		return table.getBodies()
			.getFirst()
			.getRows()
			.stream()
			.map(HtmlTableRow::getCells)
			.map((cells) -> cells.stream().map(DomNode::asNormalizedText).toList())
			.toList();
	}

}
