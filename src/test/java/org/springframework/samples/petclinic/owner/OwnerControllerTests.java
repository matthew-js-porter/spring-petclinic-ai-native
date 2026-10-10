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
import org.springframework.data.domain.PageImpl;
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
import static org.mockito.BDDMockito.given;

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

	private String body(String uri) {
		return assertThat(this.mvc.get().uri(uri)).hasStatusOk().bodyText().actual();
	}

	private static int rows(String body) {
		String tbody = body.substring(body.indexOf("<tbody>"), body.indexOf("</tbody>"));
		return tbody.split("</tr>", -1).length - 1;
	}

	private void stubOwners(List<Owner> all) {
		given(this.owners.findAll(any(Sort.class))).willReturn(all);
		given(this.owners.findAll(any(Pageable.class))).willAnswer((invocation) -> {
			Pageable pageable = invocation.getArgument(0);
			int from = (int) Math.min(pageable.getOffset(), all.size());
			int to = Math.min(from + pageable.getPageSize(), all.size());
			return new PageImpl<>(all.subList(from, to), pageable, all.size());
		});
	}

	private static Owner owner(int id, String firstName, String lastName, Pet... pets) {
		return new Owner(id, firstName, lastName, id + " Main St.", "Madison", "608555000" + id,
				new LinkedHashSet<>(Set.of(pets)));
	}

	private static Pet pet(int id, String name) {
		return new Pet(id, name, LocalDate.of(2010, 1, 1), AggregateReference.to(1));
	}

}
