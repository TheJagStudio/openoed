package gov.openoed.platform.ledger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.platform.PlatformProperties;

import org.springframework.stereotype.Component;

/**
 * Append-only ledger. Items are keyed by partition + sort key. A second put of the same key is
 * rejected. There is no update and no delete — the file is only ever appended.
 */
@Component
public class AppendOnlyLedger {

	public static final String TABLE = "audit-ledger";

	private final JsonMapper json;
	private final Path file;
	private final ConcurrentHashMap<String, Map<String, Object>> items = new ConcurrentHashMap<>();

	public AppendOnlyLedger(JsonMapper json, PlatformProperties props) {
		this.json = json;
		this.file = java.nio.file.Path.of(props.dataDir()).resolve("audit-ledger.jsonl");
		load();
	}

	public void put(String pk, String sk, Map<String, Object> attributes) {
		if (pk == null || pk.isBlank() || sk == null || sk.isBlank()) {
			throw new IllegalArgumentException("pk and sk are required");
		}
		String key = pk + "|" + sk;
		if (items.containsKey(key)) {
			throw new DuplicateItemException(pk, sk);
		}
		Map<String, Object> stored = new LinkedHashMap<>();
		stored.put("pk", pk);
		stored.put("sk", sk);
		if (attributes != null) stored.putAll(attributes);
		stored.put("pk", pk);
		stored.put("sk", sk);
		synchronized (this) {
			if (items.putIfAbsent(key, Map.copyOf(stored)) != null) {
				throw new DuplicateItemException(pk, sk);
			}
			append(stored);
		}
	}

	public List<Map<String, Object>> query(String pk, Boolean anomaly, int limit) {
		int cap = limit <= 0 ? 100 : Math.min(limit, 500);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> item : items.values()) {
			if (pk != null && !pk.isBlank() && !pk.equals(item.get("pk"))) continue;
			if (anomaly != null && anomaly != Boolean.TRUE.equals(item.get("anomaly"))) continue;
			out.add(item);
			if (out.size() >= cap) break;
		}
		out.sort((a, b) -> String.valueOf(b.get("sk")).compareTo(String.valueOf(a.get("sk"))));
		return out;
	}

	public int size() {
		return items.size();
	}

	private void load() {
		try {
			if (!Files.exists(file)) return;
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				if (line.isBlank()) continue;
				Map<String, Object> item = json.readValue(line, new TypeReference<>() {
				});
				items.put(item.get("pk") + "|" + item.get("sk"), item);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void append(Map<String, Object> item) {
		try {
			Files.createDirectories(file.getParent());
			String line = json.writeValueAsString(item) + "\n";
			Files.writeString(file, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static class DuplicateItemException extends RuntimeException {
		public DuplicateItemException(String pk, String sk) {
			super("Item already exists: " + pk + " / " + sk);
		}
	}
}
