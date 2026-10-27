package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parses the {@code yyyy-mm-dd} text that date fields bind as.
 *
 * @author Matthew Porter
 */
final class IsoDates {

	// ISO_LOCAL_DATE resolves strictly, so 2024-02-30 is rejected rather than adjusted.
	// It also reads signed and five-digit years, which the pattern rules out first.
	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

	private static final Pattern SHAPE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

	private IsoDates() {
	}

	/**
	 * Parses a date typed as {@code yyyy-mm-dd}.
	 * @param text the bound field value
	 * @return the date, or empty if {@code text} is not a real date in the form
	 * {@code yyyy-mm-dd}
	 */
	static Optional<LocalDate> parse(String text) {
		if (!SHAPE.matcher(text).matches()) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalDate.parse(text, FORMAT));
		}
		catch (DateTimeParseException ex) {
			return Optional.empty();
		}
	}

}
