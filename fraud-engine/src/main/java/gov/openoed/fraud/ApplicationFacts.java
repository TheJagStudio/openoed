package gov.openoed.fraud;

import java.util.List;

/** Everything the rules engine is allowed to see. Callers load history; the engine stays pure. */
public record ApplicationFacts(
		String serialNumber,
		String mark,
		int internationalClass,
		String goodsDescription,
		String ownerName,
		String ownerAddress,
		String ownerEmail,
		String attorneyName,
		String attorneyBarNumber,
		SpecimenFacts specimen,
		List<PriorFiling> history) {

	public ApplicationFacts {
		history = history == null ? List.of() : List.copyOf(history);
		specimen = specimen == null ? SpecimenFacts.missing() : specimen;
		mark = mark == null ? "" : mark;
		goodsDescription = goodsDescription == null ? "" : goodsDescription;
		ownerName = ownerName == null ? "" : ownerName;
		ownerAddress = ownerAddress == null ? "" : ownerAddress;
		ownerEmail = ownerEmail == null ? "" : ownerEmail;
		attorneyName = attorneyName == null ? "" : attorneyName;
		attorneyBarNumber = attorneyBarNumber == null ? "" : attorneyBarNumber;
	}
}
