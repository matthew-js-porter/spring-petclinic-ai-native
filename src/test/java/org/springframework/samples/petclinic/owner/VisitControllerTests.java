package org.springframework.samples.petclinic.owner;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@WebMvcTest(VisitController.class)
class VisitControllerTests {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository petTypes;

	@MockitoBean
	private VisitRepository visits;

	@TestBean
	private Clock clock;

	static Clock clock() {
		return Clock.fixed(TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
	}

	@BeforeEach
	void setUp() {
		given(this.owners.findById(anyInt())).willReturn(Optional.empty());
		stubOwner(owner(1, "George", "Franklin", pet(1, "Leo", 1)));
		stubOwner(owner(6, "Jean", "Coleman", pet(7, "Samantha", 1), pet(8, "Max", 1)));
		given(this.petTypes.findAll()).willReturn(List.of(new PetType(1, "cat"), new PetType(2, "dog")));
		given(this.visits.findByPetIds(List.of(7))).willReturn(List.of(visit(1, 7, "2013-01-01", "rabies shot"),
				visit(4, 7, "2013-01-04", "spayed"), visit(5, 7, "2013-01-04", "checkup")));
	}

	@Test
	void showsTheNewVisitFormWithTodayAndThePetsVisits() {
		MvcTestResult result = this.mvc.get().uri("/owners/6/pets/7/visits/new.html").exchange();

		assertThat(result).hasStatusOk()
			.hasContentTypeCompatibleWith(MediaType.TEXT_HTML)
			.hasViewName("owners/visitForm")
			.model()
			.containsEntry("visitForm", new VisitForm("2026-10-10", ""))
			.containsEntry("petName", "Samantha")
			.containsEntry("petType", "cat")
			.containsEntry("ownerName", "Jean Coleman")
			.containsEntry("visits",
					List.of(new VisitView(5, LocalDate.of(2013, 1, 4), "checkup"),
							new VisitView(4, LocalDate.of(2013, 1, 4), "spayed"),
							new VisitView(1, LocalDate.of(2013, 1, 1), "rabies shot")));
		assertThat(result).bodyText()
			.contains("<title>New Visit · Spring PetClinic</title>", "<h2 class=\"pc-page-title\">New Visit</h2>",
					"<dl class=\"pc-detail pc-context\">", "<dd>Samantha</dd>", "<dd>cat</dd>", "<dd>Jean Coleman</dd>",
					"<form method=\"post\" action=\"/owners/6/pets/7/visits\" novalidate", "value=\"2026-10-10\"",
					"maxlength=\"255\"", ">Add Visit</button>", "<h3 class=\"pc-section-title\">Previous Visits</h3>",
					"<caption class=\"pc-sr-only\">Visits of Samantha</caption>")
			.containsPattern("(?s)checkup.*spayed.*rabies shot")
			.containsPattern("href=\"/owners.html\"\\s+aria-current=\"page\"")
			.doesNotContain("max=\"", "role=\"alert\"", "pc-field-error", "aria-invalid");
	}

	@Test
	void showsTheEmptySentenceForAPetWithoutVisits() {
		assertThat(this.mvc.get().uri("/owners/6/pets/8/visits/new.html")).hasStatusOk()
			.bodyText()
			.contains("<p class=\"pc-muted pc-empty\">This pet has no visits.</p>", "<dd>Max</dd>")
			.doesNotContain("<table");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/owners/999/pets/7/visits/new.html", "/owners/0/pets/1/visits/new.html",
			"/owners/6/pets/999/visits/new.html", "/owners/1/pets/7/visits/new.html" })
	void returnsNotFoundForAnUnknownOwnerOrPet(String path) {
		assertThat(this.mvc.get().uri(path)).hasStatus(HttpStatus.NOT_FOUND).bodyText().doesNotContain("Exception");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/owners/999/pets/7/visits", "/owners/0/pets/1/visits", "/owners/6/pets/999/visits",
			"/owners/1/pets/7/visits" })
	void writesNothingForAnUnknownOwnerOrPet(String path) {
		assertThat(post(path, valid())).hasStatus(HttpStatus.NOT_FOUND).bodyText().doesNotContain("Exception");
		assertThat(post(path, Map.of())).hasStatus(HttpStatus.NOT_FOUND);
		assertNoWrites();
	}

	@Test
	void rejectsNonNumericIds() {
		assertThat(this.mvc.get().uri("/owners/abc/pets/7/visits/new.html")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(this.mvc.get().uri("/owners/6/pets/abc/visits/new.html")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(post("/owners/abc/pets/7/visits", valid())).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(post("/owners/6/pets/abc/visits", valid())).hasStatus(HttpStatus.BAD_REQUEST);
		assertNoWrites();
	}

	@Test
	void servesNoFormAtTheWritePath() {
		assertThat(this.mvc.get().uri("/owners/6/pets/7/visits")).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
		assertThat(post("/owners/6/pets/7/visits/new.html", valid())).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
		assertNoWrites();
	}

	@Test
	void addsAVisitAndRedirectsToTheOwner() {
		assertThat(post("/owners/6/pets/8/visits", valid())).hasStatus(HttpStatus.SEE_OTHER)
			.hasRedirectedUrl("/owners/6.html")
			.flash()
			.containsEntry("message", "Your visit has been booked");
		then(this.visits).should().insert(8, LocalDate.of(2020, 1, 2), "checkup");
	}

	@Test
	void trimsValuesBeforeSaving() {
		Map<String, String> padded = valid();
		padded.replaceAll((field, value) -> "  " + value + "\t ");

		assertThat(post("/owners/6/pets/8/visits", padded)).hasStatus(HttpStatus.SEE_OTHER);
		then(this.visits).should().insert(8, LocalDate.of(2020, 1, 2), "checkup");
	}

	@Test
	void takesIdsOnlyFromTheUrl() {
		Map<String, String> extra = valid();
		extra.put("id", "5");
		extra.put("petId", "1");
		extra.put("ownerId", "1");

		assertThat(post("/owners/6/pets/8/visits", extra)).hasRedirectedUrl("/owners/6.html");
		then(this.visits).should().insert(8, LocalDate.of(2020, 1, 2), "checkup");
	}

	@ParameterizedTest
	@ValueSource(strings = { "2026-10-09", "2026-10-10", "2026-10-11", "2999-01-01", "1900-01-01", "2024-02-29" })
	void acceptsAnyRealDate(String date) {
		Map<String, String> params = valid();
		params.put("date", date);

		assertThat(post("/owners/6/pets/8/visits", params)).hasStatus(HttpStatus.SEE_OTHER);
		then(this.visits).should().insert(8, LocalDate.parse(date), "checkup");
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', quoteCharacter = '"', textBlock = """
			description | ""          | Description is required.
			description | "   "       | Description is required.
			description | 256         | Description must be 255 characters or fewer.
			date        | ""          | Date is required.
			date        | "   "       | Date is required.
			date        | abc         | Date must be a date in the form yyyy-mm-dd.
			date        | 2024-02-30  | Date must be a date in the form yyyy-mm-dd.
			date        | 2023-13-01  | Date must be a date in the form yyyy-mm-dd.
			date        | 07/09/2010  | Date must be a date in the form yyyy-mm-dd.
			date        | 2010-9-7    | Date must be a date in the form yyyy-mm-dd.
			date        | -0001-01-01 | Date must be a date in the form yyyy-mm-dd.
			""")
	void redisplaysTheFormWithOneMessageForAnInvalidField(String field, String value, String message) {
		Map<String, String> params = valid();
		params.put(field, value.equals("256") ? "x".repeat(256) : value);

		MvcTestResult result = post("/owners/6/pets/7/visits", params);

		assertThat(result).hasStatusOk().hasViewName("owners/visitForm");
		assertThat(result).model().extractingBindingResult("visitForm").hasFieldErrors(field);
		assertThat(result).bodyText()
			.contains("<p class=\"pc-message\" role=\"alert\">Visit not saved: 1 field needs attention.</p>",
					"<p id=\"" + field + "-error\" class=\"pc-field-error\">" + message + "</p>",
					"action=\"/owners/6/pets/7/visits\"", "Previous Visits", "spayed")
			.containsOnlyOnce("pc-field-error\"")
			.containsPattern("href=\"/owners.html\"\\s+aria-current=\"page\"");
		assertNoWrites();
	}

	@Test
	void acceptsADescriptionOfExactly255Characters() {
		Map<String, String> params = valid();
		params.put("description", "x".repeat(255));

		assertThat(post("/owners/6/pets/8/visits", params)).hasStatus(HttpStatus.SEE_OTHER);
		then(this.visits).should().insert(8, LocalDate.of(2020, 1, 2), "x".repeat(255));
	}

	@Test
	void countsEveryInvalidFieldInTheSummary() {
		MvcTestResult result = post("/owners/6/pets/7/visits", Map.of());

		assertThat(result).hasStatusOk()
			.bodyText()
			.contains("Visit not saved: 2 fields need attention.", "Date is required.", "Description is required.")
			.doesNotContain("yyyy-mm-dd.");
		assertNoWrites();
	}

	@Test
	void keepsTheTypedValuesOnARedisplay() {
		Map<String, String> params = valid();
		params.put("date", "07/09/2010");
		params.put("description", "  annual checkup  ");

		assertThat(post("/owners/6/pets/7/visits", params)).hasStatusOk()
			.bodyText()
			.contains("value=\"07/09/2010\"", "value=\"annual checkup\"")
			.containsPattern("aria-invalid=\"true\"\\s+aria-describedby=\"date-help date-error\"");
	}

	@Test
	void escapesValuesOnARedisplay() {
		Map<String, String> params = valid();
		params.put("date", "O'Neil & \"Sons\"");
		params.put("description", "<script>alert(1)</script>");

		assertThat(post("/owners/6/pets/7/visits", params)).hasStatusOk()
			.bodyText()
			.contains("value=\"&lt;script&gt;alert(1)&lt;/script&gt;\"", "value=\"O&#39;Neil &amp; &quot;Sons&quot;\"")
			.doesNotContain("<script>alert(1)</script>");
	}

	@Test
	void escapesPreviousVisits() {
		given(this.visits.findByPetIds(List.of(8)))
			.willReturn(List.of(visit(9, 8, "2020-01-01", "<script>alert(1)</script> & \"x\" 'y'")));

		assertThat(this.mvc.get().uri("/owners/6/pets/8/visits/new.html")).hasStatusOk()
			.bodyText()
			.contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;x&quot; &#39;y&#39;")
			.doesNotContain("<script>alert(1)</script>");
	}

	private void assertNoWrites() {
		then(this.visits).should(never()).insert(anyInt(), any(), anyString());
	}

	private MvcTestResult post(String path, Map<String, String> params) {
		MockMvcTester.MockMvcRequestBuilder request = this.mvc.post().uri(path);
		params.forEach((name, value) -> request.param(name, value));
		return request.exchange();
	}

	private static Map<String, String> valid() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("date", "2020-01-02");
		params.put("description", "checkup");
		return params;
	}

	private void stubOwner(Owner owner) {
		given(this.owners.findById(owner.id())).willReturn(Optional.of(owner));
	}

	private static Owner owner(int id, String firstName, String lastName, Pet... pets) {
		return new Owner(id, firstName, lastName, id + " Main St.", "Madison", "608555000" + id,
				new LinkedHashSet<>(Set.of(pets)));
	}

	private static Pet pet(int id, String name, int typeId) {
		return new Pet(id, name, LocalDate.of(2012, 9, 4), AggregateReference.to(typeId));
	}

	private static Visit visit(int id, int petId, String date, String description) {
		return new Visit(id, petId, LocalDate.parse(date), description);
	}

}
