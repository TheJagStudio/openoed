package gov.openoed.fraud;

/** Score bands used to route filings. HIGH and CRITICAL enter the OED priority queue. */
public enum RiskBand {
	LOW,
	MEDIUM,
	HIGH,
	CRITICAL;

	public static RiskBand of(int score) {
		int capped = Math.clamp(score, 0, 100);
		if (capped >= 75) return CRITICAL;
		if (capped >= 50) return HIGH;
		if (capped >= 25) return MEDIUM;
		return LOW;
	}

	public boolean priorityQueue() {
		return this == HIGH || this == CRITICAL;
	}
}
