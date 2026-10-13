package gov.openoed.fraud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom rules engine for trademark fraud signals: shell addresses, specimen forensics,
 * AI-style goods descriptions, class mismatches, famous-mark lookalikes and barred counsel.
 * Scoring is deterministic and capped at 100.
 */
public final class FraudRules {

	public Assessment assess(ApplicationFacts facts, ReferenceData refs) {
		List<Finding> findings = new ArrayList<>();
		ReferenceData data = refs == null ? ReferenceData.empty() : refs;
		shellAddress(facts, data, findings);
		velocity(facts, findings);
		specimenReuse(facts, findings);
		specimenForensics(facts.specimen(), findings);
		aiDescription(facts, findings);
		classMismatch(facts, findings);
		famousMark(facts, data, findings);
		attorney(facts, data, findings);
		shellName(facts, findings);
		shortGoods(facts, findings);
		return Assessment.of(findings);
	}

	private static void shellAddress(ApplicationFacts facts, ReferenceData data, List<Finding> findings) {
		if (TextSignals.containsShellAddress(facts.ownerAddress(), data.shellAddresses())) {
			findings.add(new Finding("SHELL_ADDRESS", "Known shell-company address",
					"The owner address matches a virtual-office or registered-agent address on the watch list.",
					30, "HIGH"));
		}
	}

	private static void velocity(ApplicationFacts facts, List<Finding> findings) {
		String address = TextSignals.normalizeAddress(facts.ownerAddress());
		String email = facts.ownerEmail().trim().toLowerCase(Locale.ROOT);
		int sameAddress = 0;
		int sameEmail = 0;
		for (PriorFiling prior : facts.history()) {
			if (!address.isBlank() && address.equals(TextSignals.normalizeAddress(prior.address()))) sameAddress++;
			if (!email.isBlank() && email.equalsIgnoreCase(nullToEmpty(prior.ownerEmail()))) sameEmail++;
		}
		if (sameAddress >= 3) {
			findings.add(new Finding("ADDRESS_VELOCITY", "Filing burst from one address",
					sameAddress + " other applications use this normalized address.",
					sameAddress >= 5 ? 25 : 15, "HIGH"));
		}
		if (sameEmail >= 3) {
			findings.add(new Finding("FILING_BURST", "Many filings from one email",
					sameEmail + " other applications use " + facts.ownerEmail() + ".",
					14, "MEDIUM"));
		}
	}

	private static void specimenReuse(ApplicationFacts facts, List<Finding> findings) {
		String sha = facts.specimen().sha256();
		if (sha == null) return;
		List<String> serials = new ArrayList<>();
		for (PriorFiling prior : facts.history()) {
			if (sha.equalsIgnoreCase(nullToEmpty(prior.specimenSha256()))) serials.add(prior.serialNumber());
		}
		if (!serials.isEmpty()) {
			findings.add(new Finding("SPECIMEN_REUSE", "Specimen reused across applications",
					"The identical image SHA-256 was already filed on " + String.join(", ", serials) + ".",
					28, "CRITICAL"));
		}
	}

	private static void specimenForensics(SpecimenFacts specimen, List<Finding> findings) {
		if (!specimen.present()) {
			findings.add(new Finding("SPECIMEN_MISSING", "No specimen of use",
					"A use-based filing needs a specimen. None could be decoded.", 18, "HIGH"));
			return;
		}
		if (specimen.duplicateBlocks() >= 3) {
			findings.add(new Finding("SPECIMEN_CLONED", "Digitally cloned region in the specimen",
					specimen.duplicateBlocks() + " identical 16×16 blocks were pasted inside the photo.",
					22, "HIGH"));
		}
		if (specimen.flatRegionRatio() >= 0.5) {
			findings.add(new Finding("SPECIMEN_OVERLAY", "Flat digital overlay on the specimen",
					Math.round(specimen.flatRegionRatio() * 100) + "% of the image is a solid block, consistent with a pasted label.",
					18, "HIGH"));
		}
		if (specimen.jpeg() && specimen.blockiness() >= 0.55) {
			findings.add(new Finding("SPECIMEN_RECOMPRESSED", "Heavily recompressed specimen",
					"JPEG blockiness is " + round(specimen.blockiness()) + ", typical of repeated saves after editing.",
					14, "MEDIUM"));
		}
		if (specimen.jpeg() && !specimen.exifPresent() && specimen.flatRegionRatio() < 0.5) {
			findings.add(new Finding("SPECIMEN_NO_EXIF", "Photographic JPEG with no EXIF",
					"Camera metadata was stripped, which is common after a digital edit.",
					8, "LOW"));
		}
	}

	private static void aiDescription(ApplicationFacts facts, List<Finding> findings) {
		int hits = TextSignals.aiPhraseHits(facts.goodsDescription());
		if (hits >= 2) {
			int weight = Math.min(32, hits * 8);
			findings.add(new Finding("AI_DESCRIPTION", "Goods description looks machine-written",
					hits + " stock generative phrases (for example “elevate”, “seamless”, “unparalleled”) appear in the identification.",
					weight, hits >= 4 ? "HIGH" : "MEDIUM"));
		}
	}

	private static void classMismatch(ApplicationFacts facts, List<Finding> findings) {
		Integer other = GoodsCatalog.mismatch(facts.internationalClass(), facts.goodsDescription());
		if (other != null) {
			findings.add(new Finding("CLASS_MISMATCH", "Goods do not match the international class",
					"The description reads like Class " + other + " goods, but the application is in Class "
							+ facts.internationalClass() + ".",
					16, "MEDIUM"));
		}
	}

	private static void famousMark(ApplicationFacts facts, ReferenceData data, List<Finding> findings) {
		String hit = TextSignals.famousMarkHit(facts.mark(), data.famousMarks());
		if (hit != null) {
			findings.add(new Finding("FAMOUS_MARK", "Mark resembles a famous brand",
					"“" + facts.mark() + "” is a close variant of the watch-list mark " + hit + ".",
					24, "HIGH"));
		}
	}

	private static void attorney(ApplicationFacts facts, ReferenceData data, List<Finding> findings) {
		String standing = data.attorneyStanding(facts.attorneyBarNumber());
		if (standing == null) return;
		if ("SUSPENDED".equalsIgnoreCase(standing) || "EXCLUDED".equalsIgnoreCase(standing)) {
			findings.add(new Finding("ATTORNEY_BAR", "Attorney is not in good standing",
					facts.attorneyName() + " (bar " + facts.attorneyBarNumber() + ") is " + standing + " on the OED watch list.",
					26, "CRITICAL"));
		}
	}

	private static void shellName(ApplicationFacts facts, List<Finding> findings) {
		if (TextSignals.shellOwnerName(facts.ownerName())) {
			findings.add(new Finding("SHELL_OWNER", "Generic shell-company owner name",
					"“" + facts.ownerName() + "” matches a holding-company naming pattern used on known shell filings.",
					12, "MEDIUM"));
		}
	}

	private static void shortGoods(ApplicationFacts facts, List<Finding> findings) {
		if (facts.goodsDescription().trim().length() < 12) {
			findings.add(new Finding("SHORT_GOODS", "Goods identification is too thin",
					"The identification is under 12 characters, which is not a real commercial description.",
					10, "LOW"));
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String round(double v) {
		return String.format(Locale.ROOT, "%.2f", v);
	}
}
