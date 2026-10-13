package gov.openoed.fraud;

/** Image measurements produced by {@link SpecimenAnalyzer}. */
public record SpecimenFacts(
		boolean present,
		String sha256,
		String averageHash,
		int width,
		int height,
		boolean exifPresent,
		boolean jpeg,
		double blockiness,
		int duplicateBlocks,
		double flatRegionRatio) {

	public static SpecimenFacts missing() {
		return new SpecimenFacts(false, null, null, 0, 0, false, false, 0, 0, 0);
	}
}
