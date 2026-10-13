package gov.openoed.fraud;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Watch lists the rules compare against. Addresses and marks are matched after normalization,
 * so callers may pass display strings.
 */
public record ReferenceData(List<String> shellAddresses, List<String> famousMarks, Map<String, String> attorneyStatus) {

	public ReferenceData {
		shellAddresses = shellAddresses == null ? List.of() : List.copyOf(shellAddresses);
		famousMarks = famousMarks == null ? List.of() : List.copyOf(famousMarks);
		attorneyStatus = attorneyStatus == null ? Map.of() : Map.copyOf(attorneyStatus);
	}

	public static ReferenceData empty() {
		return new ReferenceData(List.of(), List.of(), Map.of());
	}

	public String attorneyStanding(String barNumber) {
		if (barNumber == null || barNumber.isBlank()) return null;
		return attorneyStatus.get(barNumber.trim().toUpperCase(Locale.ROOT));
	}
}
