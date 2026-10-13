package gov.openoed.api.audit;

import java.util.HashMap;
import java.util.Map;

/** Per-request extras (resource id, anomaly) that the audit aspect copies onto the ledger item. */
public final class AuditContext {

	private static final ThreadLocal<Map<String, String>> EXTRA = ThreadLocal.withInitial(HashMap::new);

	private AuditContext() {
	}

	public static void put(String key, String value) {
		if (value != null) EXTRA.get().put(key, value);
	}

	public static void resource(String id) {
		put("resourceId", id);
	}

	public static void anomaly(String code) {
		put("anomaly", "true");
		put("anomalyCode", code);
	}

	public static Map<String, String> snapshot() {
		return Map.copyOf(EXTRA.get());
	}

	public static void clear() {
		EXTRA.remove();
	}
}
