package org.springframework.samples.petclinic.vet;

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

@WebMvcTest(VetController.class)
class VetControllerTests {

	private static final Specialty RADIOLOGY = new Specialty(1, "radiology");

	private static final Specialty SURGERY = new Specialty(2, "surgery");

	private static final Specialty DENTISTRY = new Specialty(3, "dentistry");

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private VetRepository vets;

	@MockitoBean
	private SpecialtyRepository specialties;

	@BeforeEach
	void setUp() {
		given(this.specialties.findAll()).willReturn(List.of(RADIOLOGY, SURGERY, DENTISTRY));
		stubVets(List.of(vet(1, "James", "Carter"), vet(2, "Helen", "Leary", RADIOLOGY),
				vet(3, "Linda", "Douglas", SURGERY, DENTISTRY), vet(4, "Rafael", "Ortega", SURGERY),
				vet(5, "Henry", "Stevens", RADIOLOGY), vet(6, "Sharon", "Jenkins")));
	}

	@Test
	void showsFirstPageOfVets() {
		MvcTestResult result = this.mvc.get().uri("/vets.html").exchange();

		assertThat(result).hasStatusOk()
			.hasContentTypeCompatibleWith(MediaType.TEXT_HTML)
			.hasViewName("vets/vetList")
			.model()
			.containsKey("listVets");
		assertThat(result).bodyText()
			.contains("<td>James Carter</td>", "<td>Helen Leary</td>", "<td>radiology</td>",
					"<td>dentistry surgery</td>", ">none</td>")
			.doesNotContain("Sharon Jenkins");
	}

	@Test
	void showsOneRowPerVetOnEachPage() {
		assertThat(rows(body("/vets.html"))).isEqualTo(5);
		String page2 = body("/vets.html?page=2");
		assertThat(rows(page2)).isEqualTo(1);
		assertThat(page2).contains("<td>Sharon Jenkins</td>");
	}

	@Test
	void showsPaginationLinks() {
		String page1 = body("/vets.html");
		assertThat(page1).contains("<a href=\"/vets.html?page=2\" aria-label=\"Page 2\">2</a>")
			.contains("<span aria-current=\"page\">1</span>")
			.contains("<span class=\"pc-disabled\" aria-disabled=\"true\" title=\"First\">«</span>")
			.contains("<span class=\"pc-disabled\" aria-disabled=\"true\" title=\"Previous\">‹</span>")
			.doesNotContain("aria-label=\"First page\"", "aria-label=\"Previous page\"");

		String page2 = body("/vets.html?page=2");
		assertThat(page2).contains("<span aria-current=\"page\">2</span>")
			.contains("href=\"/vets.html?page=1\" title=\"First\" aria-label=\"First page\"")
			.doesNotContain("aria-label=\"Next page\"", "aria-label=\"Last page\"");
		assertThat(page1 + page2).doesNotContain("lastName");
	}

	@Test
	void hidesPaginationWhenThereIsOnlyOnePage() {
		stubVets(List.of(vet(1, "James", "Carter")));

		assertThat(body("/vets.html")).doesNotContain("Pages:");
	}

	@Test
	void marksVeterinariansActiveInTheNavbar() {
		assertThat(body("/vets.html")).containsPattern("href=\"/vets.html\"\\s+aria-current=\"page\"");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-1", "abc" })
	void rejectsInvalidPage(String page) {
		assertThat(this.mvc.get().uri("/vets.html?page={page}", page)).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyText()
			.doesNotContain("Exception");
	}

	@Test
	void returnsNotFoundBeyondTheLastPage() {
		assertThat(this.mvc.get().uri("/vets.html?page=3")).hasStatus(HttpStatus.NOT_FOUND)
			.bodyText()
			.doesNotContain("Exception");
	}

	@Test
	void returnsAllVetsAsJson() {
		assertThat(this.mvc.get().uri("/vets").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("""
					{"vetList":[
					 {"id":1,"firstName":"James","lastName":"Carter","specialties":[]},
					 {"id":2,"firstName":"Helen","lastName":"Leary","specialties":[{"id":1,"name":"radiology"}]},
					 {"id":3,"firstName":"Linda","lastName":"Douglas",
					  "specialties":[{"id":3,"name":"dentistry"},{"id":2,"name":"surgery"}]},
					 {"id":4},{"id":5},{"id":6,"specialties":[]}]}
					""")
			.doesNotHavePath("$.vetList[0].nrOfSpecialties")
			.doesNotHavePath("$.vetList[0].new");
	}

	@Test
	void doesNotServeHtmlFromTheJsonEndpoint() {
		assertThat(this.mvc.get().uri("/vets").accept(MediaType.TEXT_HTML)).hasStatus(HttpStatus.NOT_ACCEPTABLE);
	}

	@Test
	void isReadOnly() {
		assertThat(this.mvc.post().uri("/vets")).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(this.mvc.post().uri("/vets.html")).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
	}

	@Test
	void escapesVetNames() {
		stubVets(List.of(vet(1, "<script>alert(1)</script>", "Smith")));

		assertThat(body("/vets.html")).contains("&lt;script&gt;alert(1)&lt;/script&gt; Smith")
			.doesNotContain("<script>alert(1)</script>");
	}

	private String body(String uri) {
		return assertThat(this.mvc.get().uri(uri)).hasStatusOk().bodyText().actual();
	}

	private static int rows(String body) {
		String tbody = body.substring(body.indexOf("<tbody>"), body.indexOf("</tbody>"));
		return tbody.split("</tr>", -1).length - 1;
	}

	private void stubVets(List<Vet> all) {
		given(this.vets.findAll(any(Sort.class))).willReturn(all);
		given(this.vets.findAll(any(Pageable.class))).willAnswer((invocation) -> {
			Pageable pageable = invocation.getArgument(0);
			int from = (int) Math.min(pageable.getOffset(), all.size());
			int to = Math.min(from + pageable.getPageSize(), all.size());
			return new PageImpl<>(all.subList(from, to), pageable, all.size());
		});
	}

	private static Vet vet(int id, String firstName, String lastName, Specialty... specialties) {
		Set<VetSpecialty> links = new LinkedHashSet<>();
		for (Specialty specialty : specialties) {
			links.add(new VetSpecialty(AggregateReference.to(specialty.id())));
		}
		return new Vet(id, firstName, lastName, links);
	}

}
