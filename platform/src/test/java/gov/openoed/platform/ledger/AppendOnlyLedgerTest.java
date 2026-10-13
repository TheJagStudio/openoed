package gov.openoed.platform.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;
import gov.openoed.platform.PlatformProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppendOnlyLedgerTest {

	@TempDir
	java.nio.file.Path dir;

	@Test
	void putRejectsDuplicatesAndSurvivesReload() throws Exception {
		AppendOnlyLedger ledger = ledger();
		ledger.put("AUDIT#2026-10-12", "1", Map.of("action", "VIEW", "anomaly", true));
		assertThatThrownBy(() -> ledger.put("AUDIT#2026-10-12", "1", Map.of("action", "VIEW")))
				.isInstanceOf(AppendOnlyLedger.DuplicateItemException.class);
		assertThat(Files.readAllLines(dir.resolve("audit-ledger.jsonl"))).hasSize(1);

		AppendOnlyLedger reloaded = ledger();
		assertThat(reloaded.size()).isEqualTo(1);
		assertThat(reloaded.query("AUDIT#2026-10-12", true, 10)).hasSize(1);
		assertThat(reloaded.query("AUDIT#2026-10-12", false, 10)).isEmpty();
	}

	@Test
	void blankKeysAreRejected() {
		assertThatThrownBy(() -> ledger().put(" ", "1", Map.of())).isInstanceOf(IllegalArgumentException.class);
	}

	private AppendOnlyLedger ledger() {
		return new AppendOnlyLedger(JsonMapper.builder().build(), new PlatformProperties(dir.toString(), "http://localhost", "token"));
	}
}
