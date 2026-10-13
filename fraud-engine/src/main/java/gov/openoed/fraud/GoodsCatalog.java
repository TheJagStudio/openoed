package gov.openoed.fraud;

import java.util.Map;
import java.util.Set;

/**
 * A small class → keyword map used only to spot an obvious goods/class mismatch
 * (software described in Class 25, clothing described in Class 9, and so on).
 */
public final class GoodsCatalog {

	private static final Map<Integer, Set<String>> KEYWORDS = Map.ofEntries(
			Map.entry(3, Set.of("cream", "lotion", "cosmetic", "shampoo", "soap", "skincare", "perfume")),
			Map.entry(9, Set.of("software", "downloadable", "computer", "application", "chip", "sensor", "headphone")),
			Map.entry(16, Set.of("paper", "notebook", "printed", "poster", "pen")),
			Map.entry(21, Set.of("mug", "kettle", "cookware", "brush", "utensil")),
			Map.entry(25, Set.of("shirt", "clothing", "apparel", "footwear", "sneaker", "jacket", "hat")),
			Map.entry(30, Set.of("coffee", "tea", "chocolate", "bakery", "spice", "candy")),
			Map.entry(32, Set.of("beer", "water", "juice", "soda", "beverage")),
			Map.entry(41, Set.of("education", "entertainment", "training", "workshop"))
	);

	private GoodsCatalog() {
	}

	/** Returns the foreign class whose keywords dominate, or empty when the stated class fits. */
	public static Integer mismatch(int statedClass, String goods) {
		Set<String> tokens = TextSignals.tokens(goods);
		int own = overlap(statedClass, tokens);
		Integer bestOther = null;
		int best = 0;
		for (int cls : KEYWORDS.keySet()) {
			if (cls == statedClass) continue;
			int n = overlap(cls, tokens);
			if (n > best) {
				best = n;
				bestOther = cls;
			}
		}
		if (best >= 2 && best > own) return bestOther;
		return null;
	}

	private static int overlap(int cls, Set<String> tokens) {
		Set<String> words = KEYWORDS.get(cls);
		if (words == null) return 0;
		int n = 0;
		for (String t : tokens) {
			if (words.contains(t) || (t.endsWith("s") && words.contains(t.substring(0, t.length() - 1)))) n++;
		}
		return n;
	}
}
