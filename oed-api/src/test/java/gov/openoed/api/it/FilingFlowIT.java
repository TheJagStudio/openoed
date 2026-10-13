package gov.openoed.api.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;
import javax.imageio.ImageIO;
import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FilingFlowIT {

	@Autowired MockMvc mvc;
	@Autowired JsonMapper json;
	@Autowired UserRepository users;
	@Autowired PasswordEncoder passwords;

	@Test
	void shellFilingIsScoredAndQueued() throws Exception {
		String email = "it-" + System.nanoTime() + "@openoed.test";
		mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"Applicant#12345","displayName":"IT User"}
						""".formatted(email))).andExpect(status().isCreated());
		MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"Applicant#12345"}
						""".formatted(email))).andExpect(status().isOk()).andReturn();
		String token = json.readTree(login.getResponse().getContentAsString()).path("accessToken").asString();

		String meta = """
				{"mark":"AURORA LUMEN","markType":"STANDARD","internationalClass":3,
				"goodsDescription":"Elevate your everyday with our cutting-edge, seamless, unparalleled elixir designed to transform your routine.",
				"ownerName":"Global Prime Holdings LLC","ownerAddress":"1234 Market Street, Suite 400, Wilmington, DE 19801",
				"ownerEmail":"filings@prime.example","attorneyName":"Morgan Shell","attorneyBarNumber":"99999"}
				""";
		MvcResult filed = mvc.perform(multipart("/api/v1/applications")
				.file(new MockMultipartFile("application", "application.json", "application/json", meta.getBytes()))
				.file(new MockMultipartFile("specimen", "specimen.png", "image/png", png()))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isCreated()).andReturn();
		JsonNode body = json.readTree(filed.getResponse().getContentAsString());
		assertThat(body.path("riskBand").asString()).isIn("HIGH", "CRITICAL");
		assertThat(body.path("riskScore").asInt()).isGreaterThanOrEqualTo(50);
		assertThat(body.path("review").path("status").asString()).isEqualTo("OPEN");

		String id = body.path("id").asString();
		mvc.perform(get("/api/v1/applications/" + id).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/applications/" + id + "/specimen").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/public/stats")).andExpect(status().isOk());

		String examinerToken = tokenFor(Role.EXAMINER, "L2");
		String oedToken = tokenFor(Role.OED, null);
		MvcResult issued = mvc.perform(post("/api/v1/applications/" + id + "/office-actions")
				.header("Authorization", "Bearer " + examinerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\":\"NONFINAL\",\"body\":\"Submit a substitute specimen.\"}"))
				.andExpect(status().isOk()).andReturn();
		String officeActionId = json.readTree(issued.getResponse().getContentAsString()).path("officeActions").path(0).path("id").asString();
		mvc.perform(post("/api/v1/reviews/" + id).header("Authorization", "Bearer " + oedToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"decision\":\"CLEAR\",\"notes\":\"specimen is genuine\"}")).andExpect(status().isOk());
		MvcResult abandoned = mvc.perform(post("/internal/v1/deadlines/evaluate").header("X-Internal-Token", "dev-internal-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"officeActionId\":\"" + officeActionId + "\"}")).andExpect(status().isOk()).andReturn();
		JsonNode notice = json.readTree(abandoned.getResponse().getContentAsString()).path("notice");
		mvc.perform(post("/internal/v1/notices").header("X-Internal-Token", "dev-internal-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(notice))).andExpect(status().isOk());
		mvc.perform(post("/internal/v1/deadlines/evaluate").header("X-Internal-Token", "dev-internal-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"officeActionId\":\"" + officeActionId + "\"}")).andExpect(status().isOk());
		mvc.perform(post("/api/v1/office-actions/" + officeActionId + "/simulate-deadline")
				.header("Authorization", "Bearer " + examinerToken)).andExpect(status().isConflict());

		String second = file(token, "HARBOR SOAP", 3, "Bar soap and hand lotion", "Harbor Soap Co.",
				"18 Wharf Lane, Bath, ME 04530", "hello@harbor.example");
		MvcResult secondAction = mvc.perform(post("/api/v1/applications/" + second + "/office-actions")
				.header("Authorization", "Bearer " + examinerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"type\":\"NONFINAL\",\"body\":\"Please clarify the goods.\"}")).andExpect(status().isOk()).andReturn();
		String secondOfficeAction = json.readTree(secondAction.getResponse().getContentAsString()).path("officeActions").path(0).path("id").asString();
		mvc.perform(post("/api/v1/office-actions/" + secondOfficeAction + "/responses")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"body\":\"The goods are bar soap sold in our shop.\"}")).andExpect(status().isOk());
		mvc.perform(post("/internal/v1/deadlines/evaluate").header("X-Internal-Token", "dev-internal-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"officeActionId\":\"" + secondOfficeAction + "\"}")).andExpect(status().isOk());
		mvc.perform(post("/api/v1/applications/" + second + "/approve").header("Authorization", "Bearer " + examinerToken))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/reviews?status=OPEN").header("Authorization", "Bearer " + oedToken)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/deadlines").header("Authorization", "Bearer " + examinerToken)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/notices").header("Authorization", "Bearer " + examinerToken)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/system").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/audit").header("Authorization", "Bearer " + oedToken)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/dashboard").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}")).andExpect(status().isForbidden());
		MvcResult again = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"Applicant#12345\"}")).andExpect(status().isOk()).andReturn();
		String refresh = json.readTree(again.getResponse().getContentAsString()).path("refreshToken").asString();
		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh + "\"}")).andExpect(status().isOk());
		mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"not-a-token\"}")).andExpect(status().isNoContent());
	}

	private String file(String token, String mark, int cls, String goods, String owner, String address, String mail) throws Exception {
		String meta = """
				{"mark":"%s","markType":"STANDARD","internationalClass":%d,"goodsDescription":"%s",
				"ownerName":"%s","ownerAddress":"%s","ownerEmail":"%s"}
				""".formatted(mark, cls, goods, owner, address, mail);
		MvcResult filed = mvc.perform(multipart("/api/v1/applications")
				.file(new MockMultipartFile("application", "application.json", "application/json", meta.getBytes()))
				.file(new MockMultipartFile("specimen", "specimen.png", "image/png", png()))
				.header("Authorization", "Bearer " + token)).andExpect(status().isCreated()).andReturn();
		return json.readTree(filed.getResponse().getContentAsString()).path("id").asString();
	}

	private String tokenFor(Role role, String artUnit) throws Exception {
		String email = role.name().toLowerCase() + "-" + System.nanoTime() + "@openoed.test";
		UserAccount user = new UserAccount();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setDisplayName(role.name());
		user.setRole(role);
		user.setArtUnit(artUnit);
		user.setPasswordHash(passwords.encode("Examiner#12345"));
		user.setEnabled(true);
		user.setCreatedAt(Instant.now());
		users.save(user);
		MvcResult login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"Examiner#12345\"}")).andExpect(status().isOk()).andReturn();
		return json.readTree(login.getResponse().getContentAsString()).path("accessToken").asString();
	}

	private static byte[] png() throws Exception {
		BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) image.setRGB(x, y, new Color(20 + x, 40 + y, 80).getRGB());
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}
}
