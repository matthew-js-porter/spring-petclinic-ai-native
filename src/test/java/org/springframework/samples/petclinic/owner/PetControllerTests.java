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

@WebMvcTest(PetController.class)
class PetControllerTests {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository petTypes;

	@MockitoBean
	private PetRepository pets;

	@TestBean
	private Clock clock;

	static Clock clock() {
		return Clock.fixed(TODAY.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
	}

	@BeforeEach
	void setUp() {
		given(this.owners.findById(anyInt())).willReturn(Optional.empty());
		stubOwner(owner(1, "George", "Franklin", pet(1, "Leo", LocalDate.of(2010, 9, 7), 1)));
		stubOwner(owner(2, "Betty", "Davis", pet(2, "Basil", LocalDate.of(2012, 8, 6), 6)));
		stubOwner(owner(6, "Jean", "Coleman", pet(7, "Samantha", LocalDate.of(2012, 9, 4), 1),
				pet(8, "Max", LocalDate.of(2012, 9, 4), 1)));
		stubOwner(owner(11, "Helen", "Ostrander"));
		given(this.petTypes.findAll()).willReturn(List.of(new PetType(1, "cat"), new PetType(2, "dog"),
				new PetType(3, "lizard"), new PetType(4, "snake"), new PetType(5, "bird"), new PetType(6, "hamster")));
		given(this.pets.update(anyInt(), anyInt(), anyString(), any(), anyInt())).willReturn(true);
	}

	@Test
	void showsABlankNewPetForm() {
		MvcTestResult result = this.mvc.get().uri("/owners/1/pets/new.html").exchange();

		assertThat(result).hasStatusOk()
			.hasContentTypeCompatibleWith(MediaType.TEXT_HTML)
			.hasViewName("owners/petForm")
			.model()
			.containsEntry("petForm", PetForm.empty());
		assertThat(result).bodyText()
			.contains("<title>New Pet · Spring PetClinic</title>", ">New Pet</h2>",
					"<p class=\"pc-muted\">Owner: <span>George Franklin</span></p>",
					"<form method=\"post\" action=\"/owners/1/pets\" novalidate", "max=\"2026-10-10\"",
					"<option value=\"\" selected=\"selected\">Select a type</option>", ">Add Pet</button>")
			.containsPattern("(?s)>bird<.*>cat<.*>dog<.*>hamster<.*>lizard<.*>snake<")
			.containsPattern("href=\"/owners.html\"\\s+aria-current=\"page\"")
			.containsOnlyOnce("selected=\"selected\"")
			.doesNotContain("role=\"alert\"", "pc-field-error", "aria-invalid");
	}

	@Test
	void offersAFirstPetToAnOwnerWithNone() {
		assertThat(this.mvc.get().uri("/owners/11/pets/new.html")).hasStatusOk()
			.bodyText()
			.contains("Owner: <span>Helen Ostrander</span>", "action=\"/owners/11/pets\"");
	}

	@Test
	void showsTheEditFormFilledWithThePet() {
		MvcTestResult result = this.mvc.get().uri("/owners/1/pets/1/edit.html").exchange();

		assertThat(result).hasStatusOk()
			.hasViewName("owners/petForm")
			.model()
			.containsEntry("petForm", new PetForm("Leo", "2010-09-07", "1"));
		assertThat(result).bodyText()
			.contains("<title>Edit Pet · Spring PetClinic</title>", ">Edit Pet</h2>", "value=\"Leo\"",
					"value=\"2010-09-07\"", "<option value=\"1\" selected=\"selected\">cat</option>",
					"action=\"/owners/1/pets/1\"", ">Update Pet</button>")
			.containsOnlyOnce("selected=\"selected\"");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/owners/999/pets/new.html", "/owners/0/pets/new.html", "/owners/-1/pets/new.html",
			"/owners/999/pets/1/edit.html", "/owners/1/pets/999/edit.html", "/owners/1/pets/2/edit.html" })
	void returnsNotFoundForAnUnknownOwnerOrPet(String path) {
		assertThat(this.mvc.get().uri(path)).hasStatus(HttpStatus.NOT_FOUND).bodyText().doesNotContain("Exception");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/owners/999/pets", "/owners/0/pets", "/owners/-1/pets", "/owners/999/pets/1",
			"/owners/1/pets/999", "/owners/1/pets/2" })
	void writesNothingForAnUnknownOwnerOrPet(String path) {
		assertThat(post(path, valid())).hasStatus(HttpStatus.NOT_FOUND).bodyText().doesNotContain("Exception");
		assertThat(post(path, Map.of())).hasStatus(HttpStatus.NOT_FOUND);
		assertNoWrites();
	}

	@Test
	void rejectsNonNumericIds() {
		assertThat(this.mvc.get().uri("/owners/abc/pets/new.html")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(this.mvc.get().uri("/owners/1/pets/abc/edit.html")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(post("/owners/abc/pets", valid())).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(post("/owners/1/pets/abc", valid())).hasStatus(HttpStatus.BAD_REQUEST);
		assertNoWrites();
	}

	/**
	 * Writes go to the resource paths only. A POST to the New Pet page reaches
	 * {@code POST /owners/{ownerId}/pets/{petId}} with a non-numeric pet id, so it is a 400.
	 */
	@Test
	void writesNothingForAPostToAnHtmlPage() {
		assertThat(post("/owners/1/pets/new.html", valid())).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(post("/owners/1/pets/1/edit.html", valid())).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
		assertNoWrites();
	}

	@Test
	void addsAPetAndRedirectsToTheOwner() {
		assertThat(post("/owners/1/pets", valid())).hasStatus(HttpStatus.SEE_OTHER)
			.hasRedirectedUrl("/owners/1.html")
			.flash()
			.containsEntry("message", "New Pet has been Added");
		then(this.pets).should().insert(1, "Rex", LocalDate.of(2020, 1, 2), 2);
		then(this.pets).should(never()).update(anyInt(), anyInt(), anyString(), any(), anyInt());
	}

	@Test
	void updatesAPetAndRedirectsToTheOwner() {
		assertThat(post("/owners/1/pets/1", valid())).hasStatus(HttpStatus.SEE_OTHER)
			.hasRedirectedUrl("/owners/1.html")
			.flash()
			.containsEntry("message", "Pet details has been edited");
		then(this.pets).should().update(1, 1, "Rex", LocalDate.of(2020, 1, 2), 2);
		then(this.pets).should(never()).insert(anyInt(), anyString(), any(), anyInt());
	}

	@Test
	void returnsNotFoundWhenThePetIsGoneByTheTimeItIsUpdated() {
		given(this.pets.update(anyInt(), anyInt(), anyString(), any(), anyInt())).willReturn(false);

		assertThat(post("/owners/1/pets/1", valid())).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void trimsValuesBeforeSaving() {
		Map<String, String> padded = valid();
		padded.replaceAll((field, value) -> "  " + value + "\t ");

		assertThat(post("/owners/1/pets", padded)).hasStatus(HttpStatus.SEE_OTHER);
		then(this.pets).should().insert(1, "Rex", LocalDate.of(2020, 1, 2), 2);
	}

	@Test
	void takesIdsOnlyFromTheUrl() {
		Map<String, String> extra = valid();
		extra.put("id", "5");
		extra.put("ownerId", "2");
		extra.put("owner", "2");
		extra.put("type", "3");
		extra.put("petId", "2");

		assertThat(post("/owners/1/pets", extra)).hasRedirectedUrl("/owners/1.html");
		then(this.pets).should().insert(1, "Rex", LocalDate.of(2020, 1, 2), 2);
		assertThat(post("/owners/1/pets/1", extra)).hasRedirectedUrl("/owners/1.html");
		then(this.pets).should().update(1, 1, "Rex", LocalDate.of(2020, 1, 2), 2);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', quoteCharacter = '"', textBlock = """
			name      | ""          | Name is required.
			name      | "   "       | Name is required.
			name      | 31          | Name must be 30 characters or fewer.
			name      | Leo         | This owner already has a pet with this name.
			name      | leo         | This owner already has a pet with this name.
			name      | " LEO "     | This owner already has a pet with this name.
			birthDate | ""          | Birth Date is required.
			birthDate | "   "       | Birth Date is required.
			birthDate | 2999-01-01  | Birth Date cannot be in the future.
			birthDate | 2026-10-11  | Birth Date cannot be in the future.
			birthDate | 2024-02-30  | Birth Date must be a date in the form yyyy-mm-dd.
			birthDate | 2023-13-01  | Birth Date must be a date in the form yyyy-mm-dd.
			birthDate | 07/09/2010  | Birth Date must be a date in the form yyyy-mm-dd.
			birthDate | 2010-9-7    | Birth Date must be a date in the form yyyy-mm-dd.
			birthDate | -0001-01-01 | Birth Date must be a date in the form yyyy-mm-dd.
			birthDate | abc         | Birth Date must be a date in the form yyyy-mm-dd.
			typeId    | ""          | Type is required.
			typeId    | 0           | Type is required.
			typeId    | 7           | Type is required.
			typeId    | abc         | Type is required.
			typeId    | 2.0         | Type is required.
			""")
	void redisplaysTheNewPetFormWithOneMessageForAnInvalidField(String field, String value, String message) {
		Map<String, String> params = valid();
		params.put(field, value.equals("31") ? "x".repeat(31) : value);

		MvcTestResult result = post("/owners/1/pets", params);

		assertThat(result).hasStatusOk().hasViewName("owners/petForm");
		assertThat(result).model().extractingBindingResult("petForm").hasFieldErrors(field);
		assertThat(result).bodyText()
			.contains("<p class=\"pc-message\" role=\"alert\">Pet not saved: 1 field needs attention.</p>",
					"<p id=\"" + field + "-error\" class=\"pc-field-error\">" + message + "</p>",
					"action=\"/owners/1/pets\"")
			.containsOnlyOnce("pc-field-error\"")
			.containsPattern("href=\"/owners.html\"\\s+aria-current=\"page\"");
		assertNoWrites();
	}

	@ParameterizedTest
	@ValueSource(strings = { "2026-10-10", "1900-01-01", "2024-02-29" })
	void acceptsTodayAndPastDates(String birthDate) {
		Map<String, String> params = valid();
		params.put("birthDate", birthDate);

		assertThat(post("/owners/1/pets", params)).hasStatus(HttpStatus.SEE_OTHER);
		then(this.pets).should().insert(1, "Rex", LocalDate.parse(birthDate), 2);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Leo", "LEO" })
	void letsAPetKeepItsOwnName(String name) {
		Map<String, String> params = valid();
		params.put("name", name);

		assertThat(post("/owners/1/pets/1", params)).hasStatus(HttpStatus.SEE_OTHER);
		then(this.pets).should().update(1, 1, name, LocalDate.of(2020, 1, 2), 2);
	}

	@Test
	void rejectsRenamingAPetToAnotherOfTheOwnersPets() {
		Map<String, String> params = valid();
		params.put("name", "samantha");

		assertThat(post("/owners/6/pets/8", params)).hasStatusOk()
			.bodyText()
			.contains("This owner already has a pet with this name.", "action=\"/owners/6/pets/8\"",
					">Update Pet</button>");
		assertNoWrites();
	}

	@Test
	void allowsTheSameNameOnAnotherOwner() {
		Map<String, String> params = valid();
		params.put("name", "Leo");

		assertThat(post("/owners/2/pets", params)).hasStatus(HttpStatus.SEE_OTHER);
		then(this.pets).should().insert(2, "Leo", LocalDate.of(2020, 1, 2), 2);
	}

	@Test
	void countsEveryInvalidFieldInTheSummary() {
		MvcTestResult result = post("/owners/1/pets", Map.of());

		assertThat(result).hasStatusOk()
			.bodyText()
			.contains("Pet not saved: 3 fields need attention.", "Name is required.", "Birth Date is required.",
					"Type is required.")
			.doesNotContain("yyyy-mm-dd.", "in the future");
		assertNoWrites();
	}

	@Test
	void keepsTheTypedValuesOnARedisplay() {
		Map<String, String> params = valid();
		params.put("name", "  Rex  ");
		params.put("birthDate", "07/09/2010");
		params.put("typeId", "5");

		assertThat(post("/owners/1/pets", params)).hasStatusOk()
			.bodyText()
			.contains("value=\"Rex\"", "value=\"07/09/2010\"", "<option value=\"5\" selected=\"selected\">bird</option>")
			.containsPattern("aria-invalid=\"true\"\\s+aria-describedby=\"birthDate-help birthDate-error\"")
			.containsOnlyOnce("selected=\"selected\"");
	}

	@Test
	void escapesValuesOnARedisplay() {
		Map<String, String> params = valid();
		params.put("name", "<script>alert(1)</script>");
		params.put("birthDate", "O'Neil & \"Sons\"");

		assertThat(post("/owners/1/pets", params)).hasStatusOk()
			.bodyText()
			.contains("value=\"&lt;script&gt;alert(1)&lt;/script&gt;\"", "value=\"O&#39;Neil &amp; &quot;Sons&quot;\"")
			.doesNotContain("<script>alert(1)</script>");
	}

	private void assertNoWrites() {
		then(this.pets).should(never()).insert(anyInt(), anyString(), any(), anyInt());
		then(this.pets).should(never()).update(anyInt(), anyInt(), anyString(), any(), anyInt());
	}

	private MvcTestResult post(String path, Map<String, String> params) {
		MockMvcTester.MockMvcRequestBuilder request = this.mvc.post().uri(path);
		params.forEach((name, value) -> request.param(name, value));
		return request.exchange();
	}

	private static Map<String, String> valid() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("name", "Rex");
		params.put("birthDate", "2020-01-02");
		params.put("typeId", "2");
		return params;
	}

	private void stubOwner(Owner owner) {
		given(this.owners.findById(owner.id())).willReturn(Optional.of(owner));
	}

	private static Owner owner(int id, String firstName, String lastName, Pet... pets) {
		return new Owner(id, firstName, lastName, id + " Main St.", "Madison", "608555000" + id,
				new LinkedHashSet<>(Set.of(pets)));
	}

	private static Pet pet(int id, String name, LocalDate birthDate, int typeId) {
		return new Pet(id, name, birthDate, AggregateReference.to(typeId));
	}

}
