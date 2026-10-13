package gov.openoed.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextSignalsTest {

	@Test
	void normalizesAddressesAndMarks() {
		assertThat(TextSignals.normalizeAddress("1234 Market St., Suite 400"))
				.isEqualTo("1234 market st suite 400");
		assertThat(TextSignals.normalizeMark("Ni-keo!")).isEqualTo("NIKEO");
	}

	@Test
	void shellAddressMatchesTheStreetEvenWhenTheSuiteDiffers() {
		assertThat(TextSignals.containsShellAddress(
				"1234 Market Street, Suite 12, Wilmington, DE 19801",
				List.of("1234 Market Street, Suite 400, Wilmington, DE 19801"))).isTrue();
		assertThat(TextSignals.containsShellAddress("18 Dock Street, Portland, ME",
				List.of("1234 Market Street, Suite 400, Wilmington, DE 19801"))).isFalse();
	}

	@Test
	void famousMarkContainsAndEditDistance() {
		assertThat(TextSignals.famousMarkHit("NIKEO", List.of("NIKE"))).isEqualTo("NIKE");
		assertThat(TextSignals.famousMarkHit("APLE", List.of("APPLE"))).isEqualTo("APPLE");
		assertThat(TextSignals.famousMarkHit("HARBOR", List.of("NIKE"))).isNull();
		assertThat(TextSignals.famousMarkHit("A", List.of("APPLE"))).isNull();
	}

	@Test
	void aiPhrasesAndShellNames() {
		assertThat(TextSignals.aiPhraseHits("Elevate your seamless unparalleled routine")).isEqualTo(3);
		assertThat(TextSignals.aiPhraseHits("Roasted coffee beans")).isZero();
		assertThat(TextSignals.shellOwnerName("Global Prime Holdings LLC")).isTrue();
		assertThat(TextSignals.shellOwnerName("Harbor Soap Co.")).isFalse();
	}

	@Test
	void classMismatchNeedsTwoForeignKeywords() {
		assertThat(GoodsCatalog.mismatch(9, "shirts jackets and footwear")).isEqualTo(25);
		assertThat(GoodsCatalog.mismatch(3, "bar soap and lotion")).isNull();
		assertThat(GoodsCatalog.mismatch(30, "coffee")).isNull();
	}

	@Test
	void levenshtein() {
		assertThat(TextSignals.levenshtein("APPLE", "APLE")).isEqualTo(1);
		assertThat(TextSignals.levenshtein("CAT", "CAT")).isZero();
	}
}
