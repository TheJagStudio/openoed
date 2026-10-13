package gov.openoed.fraud;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Phrase and name heuristics for machine-written goods descriptions and shell owners. */
public final class TextSignals {

	static final List<String> AI_PHRASES = List.of(
			"elevate your", "cutting-edge", "cutting edge", "seamless", "unparalleled",
			"holistic", "redefines", "redefine", "transformative", "delve", "tapestry",
			"as an ai", "designed to transform", "radiant confidence", "game-changing",
			"next-generation", "state-of-the-art", "meticulously crafted", "unlock your",
			"it's important to note", "robust solution", "premium ingredients");

	private static final List<String> SHELL_NAME_BITS = List.of(
			"holdings llc", "ventures llc", "global prime", "registered agents",
			"capital partners llc", "worldwide llc", "enterprises llc");

	private TextSignals() {
	}

	public static int aiPhraseHits(String text) {
		if (text == null || text.isBlank()) return 0;
		String n = text.toLowerCase(Locale.ROOT);
		int hits = 0;
		for (String phrase : AI_PHRASES) if (n.contains(phrase)) hits++;
		return hits;
	}

	public static boolean shellOwnerName(String name) {
		if (name == null) return false;
		String n = name.toLowerCase(Locale.ROOT);
		for (String bit : SHELL_NAME_BITS) if (n.contains(bit)) return true;
		return false;
	}

	public static String normalizeAddress(String address) {
		if (address == null) return "";
		return address.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().replaceAll(" +", " ");
	}

	public static String normalizeMark(String mark) {
		if (mark == null) return "";
		return mark.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
	}

	public static boolean containsShellAddress(String address, List<String> shells) {
		String n = normalizeAddress(address);
		if (n.isBlank()) return false;
		for (String shell : shells) {
			String s = normalizeAddress(shell);
			if (s.length() >= 8 && (n.contains(s) || s.contains(n))) return true;
			String street = streetLine(s);
			if (street.length() >= 10 && n.contains(street)) return true;
		}
		return false;
	}

	private static String streetLine(String normalized) {
		int comma = normalized.indexOf(" suite ");
		if (comma < 0) comma = normalized.indexOf(" ste ");
		return comma > 0 ? normalized.substring(0, comma).trim() : normalized;
	}

	/** Distance 0 or 1, or the famous mark is a prefix/infix of a longer token. */
	public static String famousMarkHit(String mark, List<String> famous) {
		String n = normalizeMark(mark);
		if (n.length() < 4) return null;
		for (String raw : famous) {
			String f = normalizeMark(raw);
			if (f.length() < 4) continue;
			if (n.equals(f) || n.contains(f) || f.contains(n)) return raw;
			if (Math.abs(n.length() - f.length()) <= 1 && levenshtein(n, f) <= 1) return raw;
		}
		return null;
	}

	static int levenshtein(String a, String b) {
		int[] prev = new int[b.length() + 1];
		int[] cur = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) prev[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			cur[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
			}
			int[] swap = prev;
			prev = cur;
			cur = swap;
		}
		return prev[b.length()];
	}

	public static Set<String> tokens(String text) {
		if (text == null || text.isBlank()) return Set.of();
		Set<String> out = new java.util.HashSet<>();
		for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
			if (!token.isBlank()) out.add(token);
		}
		return out;
	}
}
