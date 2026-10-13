package gov.openoed.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpecimenAnalyzerTest {

	private final SpecimenAnalyzer analyzer = new SpecimenAnalyzer();

	@Test
	void emptyBytesAreMissing() {
		assertThat(analyzer.analyze(null).present()).isFalse();
		assertThat(analyzer.analyze(new byte[] {1, 2, 3}).present()).isFalse();
	}

	@Test
	void jpegWithoutExifIsDetectedAndHashedStably() {
		SpecimenFacts facts = FraudRulesTest.photo(false, false, false);
		assertThat(facts.present()).isTrue();
		assertThat(facts.jpeg()).isTrue();
		assertThat(facts.exifPresent()).isFalse();
		assertThat(facts.sha256()).hasSize(64);
		assertThat(facts.averageHash()).hasSize(64);
		assertThat(facts.width()).isEqualTo(128);
	}

	@Test
	void injectedExifIsDetected() {
		SpecimenFacts facts = FraudRulesTest.photo(true, false, false);
		assertThat(facts.exifPresent()).isTrue();
	}

	@Test
	void cloneAndOverlayMeasurementsMove() {
		SpecimenFacts clean = FraudRulesTest.photo(true, false, false);
		SpecimenFacts cloned = FraudRulesTest.photo(true, true, false);
		SpecimenFacts overlay = FraudRulesTest.photo(true, false, true);
		assertThat(cloned.duplicateBlocks()).isGreaterThan(clean.duplicateBlocks());
		assertThat(overlay.flatRegionRatio()).isGreaterThan(clean.flatRegionRatio());
	}

	@Test
	void hashDistanceCountsBitDifferences() {
		assertThat(SpecimenAnalyzer.hashDistance("1100", "1100")).isZero();
		assertThat(SpecimenAnalyzer.hashDistance("1100", "1111")).isEqualTo(2);
		assertThat(SpecimenAnalyzer.hashDistance(null, "11")).isEqualTo(64);
	}

	@Test
	void nonImageBytesKeepTheDigestButAreNotPresent() {
		byte[] junk = new byte[] {(byte) 0xFF, (byte) 0xD8, 1, 2, 3, 4, 5, 6, 7, 8};
		SpecimenFacts facts = analyzer.analyze(junk);
		assertThat(facts.present()).isFalse();
		assertThat(facts.sha256()).isNotBlank();
		assertThat(facts.jpeg()).isTrue();
	}
}
