package org.springframework.samples.petclinic;

import java.io.IOException;

import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlButton;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the navbar toggle script in HtmlUnit through MockMvc, with the window at phone
 * width so the stylesheet shows the toggle (HtmlUnit evaluates the media queries and
 * ignores clicks on hidden elements). The {@link WebClient} is shared with the other page
 * tests, so its width and JavaScript setting are put back after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NavbarHtmlUnitTests {

	@Autowired
	private WebClient webClient;

	private int innerWidth;

	private int outerWidth;

	@BeforeEach
	void usePhoneWidth() {
		this.innerWidth = this.webClient.getCurrentWindow().getInnerWidth();
		this.outerWidth = this.webClient.getCurrentWindow().getOuterWidth();
		resize(375);
	}

	@AfterEach
	void restoreTheBrowser() {
		this.webClient.getCurrentWindow().setInnerWidth(this.innerWidth);
		this.webClient.getCurrentWindow().setOuterWidth(this.outerWidth);
		this.webClient.getOptions().setJavaScriptEnabled(true);
	}

	@Test
	void marksThePageOnceTheScriptHasRun() throws IOException {
		HtmlPage page = this.webClient.getPage("/vets.html");

		assertThat(page.getDocumentElement().getAttribute("class")).contains("pc-js");
		HtmlButton toggle = toggle(page);
		assertThat(toggle.getAttribute("type")).isEqualTo("button");
		assertThat(toggle.getAttribute("aria-controls")).isEqualTo("main-navbar");
		assertThat(toggle.getAttribute("aria-expanded")).isEqualTo("false");
		assertThat(toggle.getAttribute("aria-label")).isEqualTo("Toggle navigation");
		assertThat(menu(page).getAttribute("class")).doesNotContain("pc-open");
	}

	@Test
	void showsTheToggleOnlyBelowTheLargeBreakpoint() throws IOException {
		assertThat(toggle(this.webClient.getPage("/vets.html")).isDisplayed()).isTrue();

		resize(991);
		assertThat(toggle(this.webClient.getPage("/vets.html")).isDisplayed()).isTrue();

		resize(992);
		HtmlPage desktop = this.webClient.getPage("/vets.html");
		assertThat(toggle(desktop).isDisplayed()).isFalse();
		assertThat(desktop.getAnchorByHref("/owners.html").isDisplayed()).isTrue();
	}

	@Test
	void hidesTheMenuUntilTheToggleOpensIt() throws IOException {
		HtmlPage page = this.webClient.getPage("/vets.html");
		assertThat(page.getAnchorByHref("/owners.html").isDisplayed()).isFalse();

		toggle(page).click();
		assertThat(page.getAnchorByHref("/owners.html").isDisplayed()).isTrue();
	}

	@Test
	void keepsTheMenuOpenWithoutJavaScript() throws IOException {
		this.webClient.getOptions().setJavaScriptEnabled(false);
		HtmlPage page = this.webClient.getPage("/vets.html");

		assertThat(toggle(page).isDisplayed()).isFalse();
		assertThat(page.getAnchorByHref("/owners.html").isDisplayed()).isTrue();
	}

	@Test
	void opensAndClosesTheMenuOnClick() throws IOException {
		HtmlPage page = this.webClient.getPage("/vets.html");
		HtmlButton toggle = toggle(page);

		toggle.click();
		assertThat(toggle.getAttribute("aria-expanded")).isEqualTo("true");
		assertThat(menu(page).getAttribute("class")).contains("pc-open");

		toggle.click();
		assertThat(toggle.getAttribute("aria-expanded")).isEqualTo("false");
		assertThat(menu(page).getAttribute("class")).doesNotContain("pc-open");
	}

	@Test
	void closesTheMenuOnEscapeAndReturnsFocusToTheToggle() throws IOException {
		HtmlPage page = this.webClient.getPage("/vets.html");
		HtmlButton toggle = toggle(page);
		toggle.click();
		page.getAnchorByHref("/owners.html").focus();

		page.executeJavaScript("document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))");

		assertThat(toggle.getAttribute("aria-expanded")).isEqualTo("false");
		assertThat(menu(page).getAttribute("class")).doesNotContain("pc-open");
		assertThat(page.getFocusedElement()).isSameAs(toggle);
	}

	@Test
	void ignoresEscapeWhileTheMenuIsClosed() throws IOException {
		HtmlPage page = this.webClient.getPage("/vets.html");
		DomElement brand = page.querySelector("a.pc-brand");
		brand.focus();

		page.executeJavaScript("document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))");

		assertThat(toggle(page).getAttribute("aria-expanded")).isEqualTo("false");
		assertThat(page.getFocusedElement()).isSameAs(brand);
	}

	private void resize(int width) {
		this.webClient.getCurrentWindow().setInnerWidth(width);
		this.webClient.getCurrentWindow().setOuterWidth(width);
	}

	private static HtmlButton toggle(HtmlPage page) {
		return page.querySelector("nav[aria-label='Main'] button.pc-navbar-toggle");
	}

	private static DomElement menu(HtmlPage page) {
		return page.getElementById("main-navbar");
	}

}
