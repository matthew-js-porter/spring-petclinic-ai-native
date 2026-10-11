package org.springframework.samples.petclinic;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.htmlunit.HttpMethod;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.WebResponse;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives failing requests through a running server, because MockMvc does not perform the
 * container's error dispatch to {@code /error}. Browsers get the error page in the shared
 * layout with the original status; JSON clients get Boot's JSON error body. Neither carries
 * any internals.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ErrorPageIntegrationTests.FailingEndpoints.class)
class ErrorPageIntegrationTests {

	private static final String SECRET = "jdbc:h2:mem secret SELECT * FROM owners";

	private static final List<String> INTERNALS = List.of("Exception", "org.springframework", "at java", "trace",
			"does not exist", "must be 1 or greater", "jdbc", "SELECT", "secret", "Whitelabel");

	@LocalServerPort
	private int port;

	private WebClient webClient;

	@BeforeEach
	void createBrowser() {
		this.webClient = new WebClient();
		// HtmlUnit's JavaScript engine cannot parse Bootstrap 5.3's bundle; see
		// VetsPageHtmlUnitTests.
		this.webClient.getOptions().setJavaScriptEnabled(false);
		this.webClient.getOptions().setCssEnabled(false);
		this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
	}

	@AfterEach
	void closeBrowser() {
		this.webClient.close();
	}

	static Stream<Arguments> failingBrowserRequests() {
		String longLastName = "x".repeat(31);
		return Stream.of(Arguments.of("GET", "/owners/999.html", 404, "Page Not Found", "999"),
				Arguments.of("GET", "/owners/1/pets/999/edit.html", 404, "Page Not Found", "999"),
				Arguments.of("GET", "/nope", 404, "Page Not Found", "nope"),
				Arguments.of("GET", "/vets.html?page=99", 404, "Page Not Found", "page=99"),
				Arguments.of("GET", "/owners/abc.html", 400, "Bad Request", "abc"),
				Arguments.of("GET", "/owners.html?page=0", 400, "Bad Request", "page=0"),
				Arguments.of("GET", "/owners.html?lastName=" + longLastName, 400, "Bad Request", longLastName),
				Arguments.of("POST", "/vets.html", 405, "Method Not Allowed", "vets.html"),
				Arguments.of("POST", "/owners.html", 405, "Method Not Allowed", "owners.html\""),
				Arguments.of("GET", "/vets", 406, "Request Not Completed", "/vets\""),
				Arguments.of("GET", "/test-errors/conflict", 409, "Request Not Completed", "conflict"),
				Arguments.of("GET", "/test-errors/boom", 500, "Something Went Wrong", "boom"),
				Arguments.of("GET", "/test-errors/missing-view", 500, "Something Went Wrong", "missing"),
				Arguments.of("GET", "/test-errors/unavailable", 503, "Something Went Wrong", "unavailable"),
				Arguments.of("GET", "/error", 500, "Something Went Wrong", "/error"));
	}

	@ParameterizedTest(name = "{0} {1} -> {2}")
	@MethodSource("failingBrowserRequests")
	void showsTheErrorPageWithTheOriginalStatus(String method, String path, int status, String heading,
			String echo) throws IOException {
		WebRequest request = new WebRequest(new URL(url(path)), HttpMethod.valueOf(method));
		request.setAdditionalHeader("Accept", MediaType.TEXT_HTML_VALUE);
		HtmlPage page = this.webClient.getPage(request);
		WebResponse response = page.getWebResponse();

		assertThat(response.getStatusCode()).isEqualTo(status);
		assertThat(response.getContentType()).isEqualTo(MediaType.TEXT_HTML_VALUE);
		assertThat(page.getTitleText()).isEqualTo(heading + " · Spring PetClinic");
		assertThat(page.querySelectorAll("h2")).hasSize(1);
		assertThat(page.querySelectorAll("main h2.pc-page-title")).singleElement()
			.extracting(DomNode::getTextContent)
			.isEqualTo(heading);
		assertThat(page.querySelectorAll("main p.pc-error-text")).singleElement()
			.extracting(DomNode::getTextContent)
			.isEqualTo(sentenceFor(heading));

		assertThat(page.querySelectorAll("header nav[aria-label=Main]")).hasSize(1);
		assertThat(page.querySelectorAll("footer.pc-footer")).hasSize(1);
		assertThat(page.querySelectorAll("[aria-current]")).isEmpty();
		assertThat(page.querySelectorAll("[role=alert]")).isEmpty();

		List<HtmlAnchor> actions = page.querySelectorAll("main ul.pc-error-actions a.pc-action")
			.stream()
			.map(HtmlAnchor.class::cast)
			.toList();
		assertThat(actions).extracting(HtmlAnchor::getTextContent).containsExactly("Go to the Homepage", "Find Owners");
		assertThat(actions).extracting(HtmlAnchor::getHrefAttribute).containsExactly("/", "/owners.html");

		String body = response.getContentAsString();
		assertThat(body).doesNotContain(INTERNALS);
		assertThat(page.querySelector("main").getTextContent()).doesNotContain(echo)
			.doesNotContain(String.valueOf(status));
		if (status == 405) {
			assertThat(response.getResponseHeaderValue("Allow")).isNotBlank();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE })
	void unknownOwnerIsAJsonErrorForJsonClients(String accept) {
		assertJsonError("/owners/999", accept, HttpStatus.NOT_FOUND);
	}

	@Test
	void nonNumericOwnerIdIsAJsonErrorForJsonClients() {
		assertJsonError("/owners/abc", MediaType.APPLICATION_JSON_VALUE, HttpStatus.BAD_REQUEST);
	}

	@ParameterizedTest
	@ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE })
	void serverErrorIsAJsonErrorWithoutInternals(String accept) {
		assertJsonError("/test-errors/boom", accept, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private void assertJsonError(String path, String accept, HttpStatus status) {
		RestClient client = RestClient.create();
		record Result(int status, MediaType contentType, String body) {
		}
		Result result = client.get()
			.uri(URI.create(url(path)))
			.header("Accept", accept)
			.exchange((request, response) -> new Result(response.getStatusCode().value(),
					response.getHeaders().getContentType(), new String(response.getBody().readAllBytes())));

		assertThat(result.status()).isEqualTo(status.value());
		assertThat(result.contentType()).isNotNull();
		assertThat(result.contentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
		Map<String, Object> json = JsonMapper.builder().build().readValue(result.body(), new TypeReference<>() {
		});
		assertThat(json.keySet()).containsExactlyInAnyOrder("timestamp", "status", "error", "path");
		assertThat(json.get("status")).isEqualTo(status.value());
		assertThat(result.body()).doesNotContain("Exception", "org.springframework", "does not exist",
				"must be 1 or greater", "jdbc", "SELECT", "secret");
	}

	private String url(String path) {
		return "http://localhost:" + this.port + path;
	}

	private static String sentenceFor(String heading) {
		return switch (heading) {
			case "Page Not Found" ->
				"We could not find the page you asked for. It may have moved, or the address may be mistyped.";
			case "Bad Request" ->
				"The address or the details sent could not be understood. Check the link and try again.";
			case "Method Not Allowed" ->
				"This page cannot be used in that way. Go back and use the links on the page instead.";
			case "Request Not Completed" -> "The request could not be completed. Check the link and try again.";
			default -> "The clinic ran into a problem on its side. Try again in a moment.";
		};
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FailingEndpoints {

		@Bean
		TestErrorController testErrorController() {
			return new TestErrorController();
		}

	}

	@Controller
	static class TestErrorController {

		@GetMapping("/test-errors/boom")
		String boom() {
			throw new IllegalStateException(SECRET);
		}

		@GetMapping("/test-errors/missing-view")
		String missingView() {
			return "test-errors/no-such-template";
		}

		@GetMapping("/test-errors/conflict")
		String conflict() {
			throw new ResponseStatusException(HttpStatus.CONFLICT, SECRET);
		}

		@GetMapping("/test-errors/unavailable")
		String unavailable() {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, SECRET);
		}

	}

}
