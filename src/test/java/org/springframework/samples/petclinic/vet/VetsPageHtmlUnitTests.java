package org.springframework.samples.petclinic.vet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTable;
import org.htmlunit.html.HtmlTableRow;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the vets page like a browser through MockMvc's HtmlUnit integration: no server
 * is started, and pages and their assets are served by MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VetsPageHtmlUnitTests {

	@Autowired
	private WebClient webClient;

	@Test
	void showsTheFirstPageOfVets() throws IOException {
		HtmlPage page = this.webClient.getPage("/vets.html");

		assertThat(page.getTitleText()).isEqualTo("Veterinarians · Spring PetClinic");
		assertThat(rows(page)).containsExactly(List.of("James Carter", "none"), List.of("Helen Leary", "radiology"),
				List.of("Linda Douglas", "dentistry surgery"), List.of("Rafael Ortega", "surgery"),
				List.of("Henry Stevens", "radiology"));
	}

	@Test
	void loadsItsStylesheetsAndScripts() throws IOException {
		HtmlPage page = this.webClient.getPage("/vets.html");

		List<String> assets = new ArrayList<>();
		page.getElementsByTagName("link").forEach((link) -> assets.add(link.getAttribute("href")));
		page.getElementsByTagName("script").forEach((script) -> assets.add(script.getAttribute("src")));

		assertThat(assets).containsExactly("/resources/css/petclinic.css", "/resources/js/navbar.js");
		for (String asset : assets) {
			assertThat(this.webClient.getPage(page.getFullyQualifiedUrl(asset)).getWebResponse().getStatusCode())
				.as(asset)
				.isEqualTo(200);
		}
	}

	@Test
	void movesBetweenPages() throws IOException {
		HtmlPage page1 = this.webClient.getPage("/vets.html");
		DomElement pagination = page1.querySelector("nav[aria-label='Vets pages']");
		assertThat(pagination.querySelectorAll("a[aria-label='First page'], a[aria-label='Previous page']")).isEmpty();
		assertThat(pagination.querySelector("[aria-current=page]").getTextContent()).isEqualTo("1");

		HtmlPage page2 = page1.getAnchorByText("2").click();
		assertThat(page2.getUrl().toString()).endsWith("/vets.html?page=2");
		assertThat(rows(page2)).containsExactly(List.of("Sharon Jenkins", "none"));
		assertThat(page2.querySelector("nav[aria-label='Vets pages'] [aria-current=page]").getTextContent())
			.isEqualTo("2");
		assertThat(page2.querySelectorAll("a[aria-label='Next page'], a[aria-label='Last page']")).isEmpty();

		HtmlPage back = ((HtmlAnchor) page2.querySelector("a[aria-label='First page']")).click();
		assertThat(rows(back)).hasSize(5);
	}

	@Test
	void navigatesFromTheNavbar() throws IOException {
		HtmlPage page = this.webClient.getPage("/vets.html?page=2");

		HtmlAnchor vetsLink = page.querySelector("nav[aria-label='Main'] a[href='/vets.html']");
		assertThat(vetsLink.getAttribute("aria-current")).isEqualTo("page");
		assertThat(page.querySelectorAll("nav[aria-label='Main'] [aria-current]")).hasSize(1);
		HtmlPage viaNav = vetsLink.click();
		assertThat(rows(viaNav)).hasSize(5);

		HtmlAnchor homeLink = page.querySelector("nav[aria-label='Main'] .pc-nav a[href='/']");
		assertThat(homeLink.hasAttribute("aria-current")).isFalse();
		HtmlPage viaBrand = page.getAnchorByHref("/").click();
		assertThat(viaBrand.getUrl().getPath()).isEqualTo("/");
		assertThat(viaBrand.getTitleText()).isEqualTo("Welcome · Spring PetClinic");
	}

	@Test
	void rendersNoScriptInsideTheContent() throws IOException {
		HtmlPage page = this.webClient.getPage("/vets.html");

		assertThat(page.querySelectorAll("main script")).isEmpty();
	}

	private static List<List<String>> rows(HtmlPage page) {
		HtmlTable table = page.getHtmlElementById("vets");
		return table.getBodies()
			.getFirst()
			.getRows()
			.stream()
			.map(HtmlTableRow::getCells)
			.map((cells) -> cells.stream().map(DomNode::asNormalizedText).toList())
			.toList();
	}

}
