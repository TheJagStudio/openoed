package gov.openoed.fraud;

import java.util.List;

public record Assessment(int score, RiskBand band, List<Finding> findings) {

	public Assessment {
		findings = findings == null ? List.of() : List.copyOf(findings);
		score = Math.clamp(score, 0, 100);
		if (band == null) band = RiskBand.of(score);
	}

	public static Assessment of(List<Finding> findings) {
		int score = Math.min(100, findings.stream().mapToInt(Finding::weight).sum());
		return new Assessment(score, RiskBand.of(score), findings);
	}
}
