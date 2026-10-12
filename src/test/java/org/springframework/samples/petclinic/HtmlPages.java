package org.springframework.samples.petclinic;

import java.util.List;

import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;

/**
 * Reads shared parts of a page in the HtmlUnit page tests.
 */
public final class HtmlPages {

	private HtmlPages() {
	}

	/**
	 * The normalised text of every element that matches the selector, in document order.
	 */
	public static List<String> texts(HtmlPage page, String selector) {
		return page.querySelectorAll(selector).stream().map(DomNode::asNormalizedText).toList();
	}

	/**
	 * The hrefs of the main navigation items marked {@code aria-current="page"}.
	 */
	public static List<String> currentNavItems(HtmlPage page) {
		return page.querySelectorAll("nav[aria-label='Main'] [aria-current=page]")
			.stream()
			.map((link) -> ((HtmlAnchor) link).getHrefAttribute())
			.toList();
	}

}
