package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@WebMvcTest(OwnerController.class)
class OwnerControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private OwnerRepository owners;

	@BeforeEach
	void setUp() {
		stubOwners(List.of(owner(1, "George", "Franklin", pet(1, "Leo")), owner(2, "Betty", "Davis"),
				owner(3, "Eduardo", "Rodriquez", pet(3, "Rosy"), pet(4, "Jewel")), owner(4, "Harold", "Davis"),
				owner(5, "Peter", "McTavish"), owner(6, "Jean", "Coleman", pet(7, "Samantha"), pet(8, "Max"))));
	}

	@Test
	void showsFirstPageOfOwners() {
		MvcTestResult result = this.mvc.get().uri("/owners.html").exchange();

		assertThat(result).hasStatusOk()
			.hasContentTypeCompatibleWith(MediaType.TEXT_HTML)
			.hasViewName("owners/ownersList")
			.model()
			.containsKey("listOwners");
		assertThat(result).bodyText()
			.contains("<td>George Franklin</td>", "<td>1 Main St.</td>", "<td>Madison</td>",
					"<td class=\"text-nowrap\">6085550001</td>", "<td>Leo</td>", "<td>Jewel Rosy</td>")
			.doesNotContain("Jean Coleman");
	}

	@Test
	void showsOneRowPerOwnerOnEachPage() {
		assertThat(rows(body("/owners.html"))).isEqualTo(5);
		String page2 = body("/owners.html?page=2");
		assertThat(rows(page2)).isEqualTo(1);
		assertThat(page2).contains("<td>Jean Coleman</td>", "<td>Max Samantha</td>")
			.contains("<span aria-current=\"page\">2</span>");
	}

	@Test
	void showsNoneForAnOwnerWithoutPets() {
		assertThat(body("/owners.html")).contains("<td class=\"pc-muted\">none</td>");
	}

	@Test
	void linksPaginationToTheOwnersPage() {
		assertThat(body("/owners.html")).contains("aria-label=\"Owners pages\"")
			.contains("<a href=\"/owners.html?page=2\" aria-label=\"Page 2\">2</a>");
	}

	@Test
	void marksFindOwnersActiveInTheNavbar() {
		assertThat(body("/owners.html")).containsPattern("href=\"/owners.html\"\\s+aria-current=\"page\"")
			.doesNotContainPattern("href=\"/vets.html\"\\s+aria-current");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-1", "abc" })
	void rejectsInvalidPage(String page) {
		assertThat(this.mvc.get().uri("/owners.html?page={page}", page)).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyText()
			.doesNotContain("Exception");
	}

	@Test
	void returnsNotFoundBeyondTheLastPage() {
		assertThat(this.mvc.get().uri("/owners.html?page=3")).hasStatus(HttpStatus.NOT_FOUND)
			.bodyText()
			.doesNotContain("Exception");
	}

	@Test
	void returnsAllOwnersAsJson() {
		assertThat(this.mvc.get().uri("/owners").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"ownerList":[
					 {"id":1,"firstName":"George","lastName":"Franklin","address":"1 Main St.","city":"Madison",
					  "telephone":"6085550001","pets":[{"id":1,"name":"Leo"}]},
					 {"id":2,"pets":[]},
					 {"id":3,"pets":[{"id":4,"name":"Jewel"},{"id":3,"name":"Rosy"}]},
					 {"id":4},{"id":5},{"id":6}]}
					""")
			.doesNotHavePath("$.ownerList[0].pets[0].birthDate")
			.doesNotHavePath("$.ownerList[0].pets[0].type");
	}

	@Test
	void doesNotServeHtmlFromTheJsonEndpoint() {
		assertThat(this.mvc.get().uri("/owners").accept(MediaType.TEXT_HTML)).hasStatus(HttpStatus.NOT_ACCEPTABLE);
	}

	@Test
	void isReadOnly() {
		assertThat(this.mvc.post().uri("/owners")).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(this.mvc.post().uri("/owners.html")).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
	}

	@Test
	void escapesOwnerValues() {
		stubOwners(List.of(owner(1, "<script>alert(1)</script>", "Smith", pet(1, "<b>Rex</b>"))));

		assertThat(body("/owners.html")).contains("&lt;script&gt;alert(1)&lt;/script&gt; Smith", "&lt;b&gt;Rex&lt;/b&gt;")
			.doesNotContain("<script>alert(1)</script>", "<b>Rex</b>");
	}

	@Test
	void showsTheSearchFormAboveTheList() {
		String body = body("/owners.html");

		assertThat(body)
			.contains("<form method=\"get\" action=\"/owners.html\" role=\"search\" class=\"pc-form\">",
					"<label for=\"lastName\" class=\"form-label\">Last Name</label>",
					"<button type=\"submit\" class=\"btn pc-button\">Find Owner</button>")
			.containsPattern(
					"<input type=\"search\" id=\"lastName\" name=\"lastName\" maxlength=\"30\" autocomplete=\"off\"\\s+class=\"form-control pc-input\" value=\"\">");
		assertThat(body.indexOf("role=\"search\"")).isBetween(body.indexOf("pc-page-title"), body.indexOf("<table"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  " })
	void listsAllOwnersWithoutASearchTerm(String term) {
		assertThat(rows(body("/owners.html"))).isEqualTo(5);
		assertThat(body("/owners.html?lastName={term}", term)).contains("<a href=\"/owners.html?page=2\" aria-label=\"Page 2\">2</a>")
			.doesNotContain("lastName=");
		then(this.owners).should(never()).findByLastNameStartingWithIgnoreCase(anyString(), any(Pageable.class));
	}

	@Test
	void searchesByTheTrimmedLastName() {
		stubSearch("Dav", List.of(owner(2, "Betty", "Davis"), owner(4, "Harold", "Davis")));

		MvcTestResult result = this.mvc.get().uri("/owners.html?lastName={term}", " Dav ").exchange();

		assertThat(result).hasStatusOk().model().containsEntry("lastName", "Dav");
		assertThat(result).bodyText()
			.contains("<td>Betty Davis</td>", "<td>Harold Davis</td>", "value=\"Dav\"",
					"Owners with last name starting with &quot;Dav&quot;, page 1 of 1")
			.doesNotContain("George Franklin", "pc-pagination");
		then(this.owners).should()
			.findByLastNameStartingWithIgnoreCase("Dav", PageRequest.of(0, OwnerController.PAGE_SIZE, Sort.by("id")));
		then(this.owners).should(never()).findAll(any(Pageable.class));
	}

	@Test
	void showsAMessageInsteadOfTheTableWhenNothingMatches() {
		stubSearch("zzz", List.of());

		assertThat(body("/owners.html?lastName=zzz")).contains(
				"<p class=\"pc-message\" role=\"status\">No owners found with a last name starting with &quot;zzz&quot;.</p>",
				"value=\"zzz\"")
			.doesNotContain("<table", "pc-pagination");
	}

	@Test
	void showsAPlainMessageWhenThereAreNoOwnersAtAll() {
		stubOwners(List.of());

		assertThat(body("/owners.html")).contains("<p class=\"pc-message\" role=\"status\">No owners found.</p>")
			.doesNotContain("<table");
	}

	@Test
	void keepsTheSearchTermInEveryPaginationLink() {
		stubSearch("Dav", List.of(owner(1, "A", "Davis"), owner(2, "B", "Davis"), owner(3, "C", "Davis"),
				owner(4, "D", "Davis"), owner(5, "E", "Davis"), owner(6, "F", "Davis"), owner(7, "G", "Davis"),
				owner(8, "H", "Davis"), owner(9, "I", "Davis"), owner(10, "J", "Davis"), owner(11, "K", "Davis")));

		String page2 = body("/owners.html?lastName=Dav&page=2");

		assertThat(page2).contains("<a href=\"/owners.html?page=1&amp;lastName=Dav\" aria-label=\"Page 1\">1</a>",
				"<a href=\"/owners.html?page=3&amp;lastName=Dav\" aria-label=\"Page 3\">3</a>",
				"href=\"/owners.html?page=1&amp;lastName=Dav\" title=\"First\"",
				"href=\"/owners.html?page=1&amp;lastName=Dav\" title=\"Previous\"",
				"href=\"/owners.html?page=3&amp;lastName=Dav\" title=\"Next\"",
				"href=\"/owners.html?page=3&amp;lastName=Dav\" title=\"Last\"");
		String nav = page2.substring(page2.indexOf("<nav class=\"pc-pagination\""), page2.lastIndexOf("</nav>"));
		List<String> links = List.of(nav.split("href=\"", -1)).subList(1, 7);
		assertThat(nav.split("href=\"", -1)).hasSize(7);
		assertThat(links).allSatisfy((link) -> assertThat(link).startsWith("/owners.html?page=").contains("lastName=Dav\""));
	}

	@Test
	void urlEncodesTheSearchTermInPaginationLinks() {
		String term = "O'B & C";
		stubSearch(term, List.of(owner(1, "A", term), owner(2, "B", term), owner(3, "C", term), owner(4, "D", term),
				owner(5, "E", term), owner(6, "F", term)));

		assertThat(body("/owners.html?lastName={term}", term))
			.contains("href=\"/owners.html?page=2&amp;lastName=O&#39;B%20%26%20C\"");
	}

	@Test
	void returnsNotFoundBeyondTheLastPageOfASearch() {
		stubSearch("Dav", List.of(owner(2, "Betty", "Davis")));

		assertThat(this.mvc.get().uri("/owners.html?lastName=Dav&page=2")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void acceptsTheFirstPageOfAnEmptySearch() {
		stubSearch("zzz", List.of());

		assertThat(this.mvc.get().uri("/owners.html?lastName=zzz&page=1")).hasStatusOk();
	}

	@ParameterizedTest
	@ValueSource(strings = { "/owners.html", "/owners" })
	void rejectsALastNameLongerThanTheColumn(String path) {
		assertThat(this.mvc.get().uri(path + "?lastName={term}", "x".repeat(31)).accept(MediaType.ALL))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyText()
			.doesNotContain("Exception");

		stubSearch("x".repeat(30), List.of());
		assertThat(this.mvc.get().uri(path + "?lastName={term}", "x".repeat(30)).accept(MediaType.ALL))
			.hasStatusOk();
	}

	@Test
	void rejectsAnInvalidPageOfASearch() {
		assertThat(this.mvc.get().uri("/owners.html?lastName=Dav&page=0")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void escapesTheSearchTerm() {
		String term = "<script>\"x\"";
		stubSearch(term, List.of());
		assertThat(body("/owners.html?lastName={term}", term))
			.contains("value=\"&lt;script&gt;&quot;x&quot;\"", "starting with &quot;&lt;script&gt;&quot;x&quot;&quot;.")
			.doesNotContain("<script>");

		stubSearch(term, List.of(owner(1, "A", "B")));
		assertThat(body("/owners.html?lastName={term}", term))
			.contains("Owners with last name starting with &quot;&lt;script&gt;&quot;x&quot;&quot;, page 1 of 1")
			.doesNotContain("<script>");
	}

	@Test
	void filtersTheJsonByLastName() {
		stubSearch("Davis", List.of(owner(2, "Betty", "Davis"), owner(4, "Harold", "Davis")));

		assertThat(this.mvc.get().uri("/owners?lastName={term}", " Davis").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"ownerList":[{"id":2,"lastName":"Davis"},{"id":4,"lastName":"Davis"}]}
					""");
		then(this.owners).should().findByLastNameStartingWithIgnoreCase("Davis", Sort.by("id"));
		then(this.owners).should(never()).findAll(any(Sort.class));
	}

	@Test
	void returnsAnEmptyJsonListWhenNothingMatches() {
		stubSearch("zzz", List.of());

		assertThat(this.mvc.get().uri("/owners?lastName=zzz").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.isStrictlyEqualTo("{\"ownerList\":[]}");
	}

	@Test
	void returnsAllOwnersAsJsonForABlankLastName() {
		assertThat(this.mvc.get().uri("/owners?lastName={term}", " ").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.ownerList.length()")
			.isEqualTo(6);
		then(this.owners).should(never()).findByLastNameStartingWithIgnoreCase(anyString(), any(Sort.class));
	}

	private String body(String uri, Object... vars) {
		return assertThat(this.mvc.get().uri(uri, vars)).hasStatusOk().bodyText().actual();
	}

	private static int rows(String body) {
		String tbody = body.substring(body.indexOf("<tbody>"), body.indexOf("</tbody>"));
		return tbody.split("</tr>", -1).length - 1;
	}

	private void stubOwners(List<Owner> all) {
		given(this.owners.findAll(any(Sort.class))).willReturn(all);
		given(this.owners.findAll(any(Pageable.class))).willAnswer((invocation) -> page(all, invocation.getArgument(0)));
	}

	private void stubSearch(String term, List<Owner> matches) {
		given(this.owners.findByLastNameStartingWithIgnoreCase(eq(term), any(Sort.class))).willReturn(matches);
		given(this.owners.findByLastNameStartingWithIgnoreCase(eq(term), any(Pageable.class)))
			.willAnswer((invocation) -> page(matches, invocation.getArgument(1)));
	}

	private static Page<Owner> page(List<Owner> all, Pageable pageable) {
		int from = (int) Math.min(pageable.getOffset(), all.size());
		int to = Math.min(from + pageable.getPageSize(), all.size());
		return new PageImpl<>(all.subList(from, to), pageable, all.size());
	}

	private static Owner owner(int id, String firstName, String lastName, Pet... pets) {
		return new Owner(id, firstName, lastName, id + " Main St.", "Madison", "608555000" + id,
				new LinkedHashSet<>(Set.of(pets)));
	}

	private static Pet pet(int id, String name) {
		return new Pet(id, name, LocalDate.of(2010, 1, 1), AggregateReference.to(1));
	}

}
