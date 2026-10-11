package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * A visit as rendered in a pet's visit list and in the owner JSON.
 */
public record VisitView(Integer id, LocalDate date, String description) {

	/**
	 * Most recent first; visits on the same day put the later-added one first.
	 */
	static final Comparator<VisitView> MOST_RECENT_FIRST = Comparator.comparing(VisitView::date)
		.thenComparing(VisitView::id)
		.reversed();

	static List<VisitView> mostRecentFirst(Collection<Visit> visits) {
		return visits.stream()
			.map((visit) -> new VisitView(visit.id(), visit.date(), visit.description()))
			.sorted(MOST_RECENT_FIRST)
			.toList();
	}

}
