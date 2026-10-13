package gov.openoed.fraud;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class FraudRulesTest {

	private final FraudRules rules = new FraudRules();

	private final ReferenceData refs = new ReferenceData(
			List.of("1234 Market Street, Suite 400, Wilmington, DE 19801"),
			List.of("NIKE", "APPLE", "STARBUCKS"),
			Map.of("99999", "SUSPENDED"));

	@Test
	void cleanFilingScoresLowWithNoFindings() {
		Assessment a = rules.assess(facts("Harbor Soap", 3, "Bar soap and hand lotion for personal care",
				"Harbor Soap Co.", "18 Dock Street, Portland, ME 04101", "ada@harborsoap.example",
				"Ada Counsel", "12345", photo(true, false, false), List.of()), refs);
		assertThat(a.band()).isEqualTo(RiskBand.LOW);
		assertThat(a.findings()).isEmpty();
		assertThat(a.band().priorityQueue()).isFalse();
	}

	@Test
	void shellAddressAndAiCopyAndSuspendedCounselReachCritical() {
		Assessment a = rules.assess(facts("Aurora Lumen", 3,
				"Elevate your everyday with our cutting-edge, seamless, unparalleled skincare elixir designed to transform your routine.",
				"Global Prime Holdings LLC",
				"1234 Market Street, Suite 400, Wilmington, DE 19801",
				"filings@prime.example", "Morgan Shell", "99999",
				photo(false, false, false), List.of()), refs);
		assertThat(a.band()).isEqualTo(RiskBand.CRITICAL);
		assertThat(a.score()).isBetween(75, 100);
		assertThat(a.findings()).extracting(Finding::code)
				.contains("SHELL_ADDRESS", "AI_DESCRIPTION", "ATTORNEY_BAR", "SHELL_OWNER", "SPECIMEN_NO_EXIF");
		assertThat(a.band().priorityQueue()).isTrue();
	}

	@Test
	void specimenReuseAndAddressVelocityAddPoints() {
		PriorFiling prior = new PriorFiling("97000001", "a@x.example", "9 Pine Road, Austin, TX 78701", "abc");
		SpecimenFacts specimen = new SpecimenFacts(true, "abc", "0".repeat(64), 64, 64, true, true, 0.1, 0, 0.05);
		ApplicationFacts facts = facts("Pine Goods", 21, "Kitchen brushes and utensils",
				"Pine Goods", "9 Pine Road, Austin, TX 78701", "a@x.example", "", "",
				specimen, List.of(prior, prior, prior));
		Assessment a = rules.assess(facts, ReferenceData.empty());
		assertThat(a.findings()).extracting(Finding::code)
				.contains("SPECIMEN_REUSE", "ADDRESS_VELOCITY", "FILING_BURST");
	}

	@Test
	void fiveAddressHitsUseTheHigherWeight() {
		PriorFiling prior = new PriorFiling("1", "z@z.example", "1 Main", null);
		var history = List.of(prior, prior, prior, prior, prior);
		Assessment a = rules.assess(facts("M", 30, "Coffee and tea beverages", "Owner", "1 Main", "other@z.example",
				"", "", photo(true, false, false), history), ReferenceData.empty());
		Finding velocity = a.findings().stream().filter(f -> f.code().equals("ADDRESS_VELOCITY")).findFirst().orElseThrow();
		assertThat(velocity.weight()).isEqualTo(25);
	}

	@Test
	void famousMarkAndClassMismatch() {
		Assessment a = rules.assess(facts("NIKEO", 9, "Shirts jackets and footwear apparel",
				"Street LLC", "1 Oak, Denver, CO 80202", "a@b.example", "", "",
				photo(true, false, false), List.of()), refs);
		assertThat(a.findings()).extracting(Finding::code).contains("FAMOUS_MARK", "CLASS_MISMATCH");
	}

	@Test
	void clonedAndOverlaySpecimensAreFlagged() {
		Assessment cloned = rules.assess(base(photo(true, true, false)), ReferenceData.empty());
		assertThat(cloned.findings()).extracting(Finding::code).contains("SPECIMEN_CLONED");
		Assessment overlay = rules.assess(base(photo(true, false, true)), ReferenceData.empty());
		assertThat(overlay.findings()).extracting(Finding::code).contains("SPECIMEN_OVERLAY");
	}

	@Test
	void missingSpecimenAndShortGoods() {
		Assessment a = rules.assess(facts("X", 30, "tea", "Owner", "1 Main Street, Boston, MA 02101",
				"a@b.example", "", "", SpecimenFacts.missing(), List.of()), ReferenceData.empty());
		assertThat(a.findings()).extracting(Finding::code).contains("SPECIMEN_MISSING", "SHORT_GOODS");
	}

	@Test
	void scoreIsCappedAt100() {
		Assessment a = rules.assess(facts("NIKE", 25,
				"Elevate your seamless unparalleled transformative game-changing next-generation shirts jackets footwear apparel",
				"Global Prime Holdings LLC",
				"1234 Market Street Suite 400 Wilmington DE 19801",
				"a@b.example", "Morgan Shell", "99999",
				SpecimenFacts.missing(), List.of(
						new PriorFiling("1", "a@b.example", "1234 Market Street Suite 400 Wilmington DE 19801", null),
						new PriorFiling("2", "a@b.example", "1234 Market Street Suite 400 Wilmington DE 19801", null),
						new PriorFiling("3", "a@b.example", "1234 Market Street Suite 400 Wilmington DE 19801", null),
						new PriorFiling("4", "a@b.example", "1234 Market Street Suite 400 Wilmington DE 19801", null),
						new PriorFiling("5", "a@b.example", "1234 Market Street Suite 400 Wilmington DE 19801", null)
				)), refs);
		assertThat(a.score()).isEqualTo(100);
		assertThat(a.band()).isEqualTo(RiskBand.CRITICAL);
	}

	@Test
	void bandsFollowThresholds() {
		assertThat(RiskBand.of(0)).isEqualTo(RiskBand.LOW);
		assertThat(RiskBand.of(24)).isEqualTo(RiskBand.LOW);
		assertThat(RiskBand.of(25)).isEqualTo(RiskBand.MEDIUM);
		assertThat(RiskBand.of(49)).isEqualTo(RiskBand.MEDIUM);
		assertThat(RiskBand.of(50)).isEqualTo(RiskBand.HIGH);
		assertThat(RiskBand.of(74)).isEqualTo(RiskBand.HIGH);
		assertThat(RiskBand.of(75)).isEqualTo(RiskBand.CRITICAL);
		assertThat(RiskBand.of(100)).isEqualTo(RiskBand.CRITICAL);
	}

	@Test
	void nullReferenceDataIsTreatedAsEmpty() {
		Assessment a = rules.assess(base(photo(true, false, false)), null);
		assertThat(a.band()).isEqualTo(RiskBand.LOW);
	}

	private static ApplicationFacts base(SpecimenFacts specimen) {
		return facts("Harbor Soap", 3, "Bar soap and hand lotion", "Harbor Soap Co.",
				"18 Dock Street, Portland, ME 04101", "ada@harborsoap.example", "", "", specimen, List.of());
	}

	private static ApplicationFacts facts(String mark, int cls, String goods, String owner, String address,
			String email, String attorney, String bar, SpecimenFacts specimen, List<PriorFiling> history) {
		return new ApplicationFacts("97000099", mark, cls, goods, owner, address, email, attorney, bar, specimen, history);
	}

	/** JPEG product photo. clone pastes the left half onto the right. overlay paints a solid slab. */
	static SpecimenFacts photo(boolean exif, boolean clone, boolean overlay) {
		BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		for (int y = 0; y < 128; y++) {
			for (int x = 0; x < 128; x++) {
				int n = (x * 13 + y * 7) % 40;
				image.setRGB(x, y, new Color(40 + n, 90 + (x % 17), 70 + (y % 19)).getRGB());
			}
		}
		g.setColor(new Color(250, 250, 250));
		g.fillRect(18, 18, 28, 28);
		g.dispose();
		if (clone) {
			for (int y = 16; y < 80; y++) {
				for (int x = 16; x < 64; x++) image.setRGB(x + 48, y, image.getRGB(x, y));
			}
		}
		if (overlay) {
			Graphics2D o = image.createGraphics();
			o.setColor(new Color(10, 10, 200));
			o.fillRect(0, 0, 128, 80);
			o.dispose();
		}
		byte[] encoded = (clone || overlay) ? png(image) : jpeg(image);
		if (exif && !clone && !overlay) encoded = withExif(encoded);
		return new SpecimenAnalyzer().analyze(encoded);
	}

	private static byte[] png(BufferedImage image) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "png", out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static byte[] jpeg(BufferedImage image) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static byte[] withExif(byte[] jpeg) {
		byte[] header = new byte[] {
				(byte) 0xFF, (byte) 0xD8,
				(byte) 0xFF, (byte) 0xE1, 0x00, 0x08,
				'E', 'x', 'i', 'f', 0, 0
		};
		byte[] rest = new byte[jpeg.length - 2];
		System.arraycopy(jpeg, 2, rest, 0, rest.length);
		byte[] all = new byte[header.length + rest.length];
		System.arraycopy(header, 0, all, 0, header.length);
		System.arraycopy(rest, 0, all, header.length, rest.length);
		return all;
	}
}
