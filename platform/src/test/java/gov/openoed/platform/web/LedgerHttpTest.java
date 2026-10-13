package gov.openoed.platform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LedgerHttpTest {

	private static final Path DIR = Path.of(System.getProperty("java.io.tmpdir"), "openoed-ledger-" + UUID.randomUUID());

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		registry.add("app.data-dir", DIR::toString);
	}

	@Autowired
	MockMvc mvc;

	@Test
	void deleteIsRejectedAndPutAppends() throws Exception {
		mvc.perform(delete("/dynamodb/tables/audit-ledger/items")).andExpect(status().isMethodNotAllowed());
		mvc.perform(put("/dynamodb/tables/audit-ledger/items")
						.contentType("application/json")
						.content("{\"pk\":\"AUDIT#1\",\"sk\":\"a\",\"attributes\":{\"action\":\"LOGIN\"}}"))
				.andExpect(status().isCreated());
		mvc.perform(put("/dynamodb/tables/audit-ledger/items")
						.contentType("application/json")
						.content("{\"pk\":\"AUDIT#1\",\"sk\":\"a\",\"attributes\":{\"action\":\"LOGIN\"}}"))
				.andExpect(status().isConflict());
	}
}
