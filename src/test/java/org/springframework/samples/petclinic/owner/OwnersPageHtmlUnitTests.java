package org.springframework.samples.petclinic.owner;

import java.io.IOException;
import java.util.List;

import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTable;
import org.htmlunit.html.HtmlTableRow;
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
		assertThat(page.querySelectorAll("#owners tbody td:not(:first-child) a")).isEmpty();
	}

	@Test
	void linksEachOwnerNameToTheOwnersPage() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners.html");

		assertThat(page.querySelectorAll("#owners tbody td:first-child a")).hasSize(5)
			.extracting((link) -> ((HtmlAnchor) link).getHrefAttribute())
			.containsExactly("/owners/1.html", "/owners/2.html", "/owners/3.html", "/owners/4.html", "/owners/5.html");

		HtmlPage details = page.getAnchorByText("George Franklin").click();

		assertThat(details.getUrl().toString()).endsWith("/owners/1.html");
		assertThat(details.getTitleText()).isEqualTo("Owner Information · Spring PetClinic");
		assertThat(details.<DomElement>querySelector("dd").asNormalizedText()).isEqualTo("George Franklin");
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
	}

	@Test
	void showsTheOwnerWithoutPetsOnTheThirdAndLastPage() throws IOException {
		HtmlPage page2 = this.webClient.getPage("/owners.html?page=2");

		HtmlPage page3 = page2.getAnchorByText("3").click();

		assertThat(page3.getUrl().toString()).endsWith("/owners.html?page=3");
		assertThat(rows(page3)).singleElement()
			.isEqualTo(List.of("Helen Ostrander", "4120 Monroe St.", "Fitchburg", "6085554412", "none"));
		assertThat(page3.querySelectorAll("a[aria-label='Next page'], a[aria-label='Last page']")).isEmpty();
	}

	@Test
	void returnsNotFoundBeyondTheThirdPage() throws IOException {
		this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

		assertThat(this.webClient.getPage("/owners.html?page=4").getWebResponse().getStatusCode()).isEqualTo(404);
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

	@Test
	void offersALabelledLastNameSearch() throws IOException {
		HtmlPage page = this.webClient.getPage("/owners.html");

		HtmlForm form = page.querySelector("form[role=search]");
		assertThat(form.getMethodAttribute()).isEqualTo("get");
		assertThat(form.getActionAttribute()).isEqualTo("/owners.html");
		HtmlInput field = form.getInputByName("lastName");
		assertThat(field.getId()).isEqualTo("lastName");
		assertThat(field.getTypeAttribute()).isEqualTo("search");
		assertThat(field.getAttribute("maxlength")).isEqualTo("30");
		assertThat(page.<DomElement>querySelector("label[for=lastName]").asNormalizedText()).isEqualTo("Last Name");
		assertThat(form.<HtmlButton>querySelector("button[type=submit]").asNormalizedText()).isEqualTo("Find Owner");
	}

	@Test
	void findsOwnersByLastName() throws IOException {
		HtmlPage results = search("Davis");

		assertThat(results.getUrl().toString()).endsWith("/owners.html?lastName=Davis");
		assertThat(rows(results)).extracting(List::getFirst).containsExactly("Betty Davis", "Harold Davis");
		assertThat(field(results).getValue()).isEqualTo("Davis");
		assertThat(results.querySelectorAll(".pc-pagination")).isEmpty();

		HtmlPage reloaded = this.webClient.getPage(results.getUrl());
		assertThat(rows(reloaded)).extracting(List::getFirst).containsExactly("Betty Davis", "Harold Davis");
	}

	@Test
	void linksSearchResultsToTheOwnersPage() throws IOException {
		HtmlPage results = search("Davis");

		assertThat(results.querySelectorAll("#owners tbody td:first-child a"))
			.extracting((link) -> ((HtmlAnchor) link).getHrefAttribute())
			.containsExactly("/owners/2.html", "/owners/4.html");

		HtmlPage details = results.getAnchorByText("Harold Davis").click();
		assertThat(details.getUrl().toString()).endsWith("/owners/4.html");
	}

	@Test
	void ignoresCaseAndMatchesPrefixes() throws IOException {
		assertThat(rows(search("dav"))).extracting(List::getFirst).containsExactly("Betty Davis", "Harold Davis");
	}

	@Test
	void listsEveryOwnerForABlankSearch() throws IOException {
		HtmlPage results = search("");

		assertThat(rows(results)).hasSize(5);
		assertThat(results.querySelectorAll(".pc-pagination")).hasSize(1);
	}

	@Test
	void explainsWhenNoOwnerMatches() throws IOException {
		HtmlPage results = search("zzz");

		assertThat(results.<DomElement>querySelector("[role=status]").asNormalizedText())
			.isEqualTo("No owners found with a last name starting with \"zzz\".");
		assertThat(results.querySelectorAll("table, .pc-pagination")).isEmpty();
		assertThat(field(results).getValue()).isEqualTo("zzz");
	}

	@Test
	void offersAddOwnerOnEveryPageAndSearchResult() throws IOException {
		HtmlPage page1 = this.webClient.getPage("/owners.html");
		HtmlPage page3 = this.webClient.getPage("/owners.html?page=3");
		HtmlPage results = search("Davis");
		HtmlPage none = search("zzz");

		for (HtmlPage page : List.of(page1, page3, results, none)) {
			assertThat(page.querySelectorAll("main a.pc-action")).singleElement().satisfies((link) -> {
				assertThat(link.asNormalizedText()).isEqualTo("Add Owner");
				assertThat(((HtmlAnchor) link).getHrefAttribute()).isEqualTo("/owners/new.html");
			});
		}
		assertThat(page1.getAnchorByText("Add Owner").<HtmlPage>click().getTitleText())
			.isEqualTo("New Owner · Spring PetClinic");
	}

	private HtmlPage search(String lastName) throws IOException {
		HtmlPage page = this.webClient.getPage("/owners.html");
		field(page).setValue(lastName);
		return page.<HtmlButton>querySelector("form[role=search] button[type=submit]").click();
	}

	private static HtmlInput field(HtmlPage page) {
		return page.querySelector("#lastName");
	}

	private static List<String> navItems(HtmlPage page) {
		return page.querySelectorAll("nav[aria-label='Main'] .pc-nav a")
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
