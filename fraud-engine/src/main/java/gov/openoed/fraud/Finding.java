package gov.openoed.fraud;

/**
 * One rule hit. {@code weight} is the points added to the application risk score.
 */
public record Finding(String code, String title, String detail, int weight, String severity) {

	public Finding {
		if (code == null || code.isBlank()) throw new IllegalArgumentException("code");
		if (weight < 0) throw new IllegalArgumentException("weight");
		severity = severity == null ? "MEDIUM" : severity;
	}
}
