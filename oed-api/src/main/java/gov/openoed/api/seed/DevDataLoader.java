package gov.openoed.api.seed;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import javax.imageio.ImageIO;

import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.auth.UserRepository;
import gov.openoed.api.office.service.OfficeActionService;
import gov.openoed.api.review.service.ReviewService;
import gov.openoed.api.trademark.ApplicationRepository;
import gov.openoed.api.trademark.ApplicationViews.ApplicationDetail;
import gov.openoed.api.trademark.service.FilingService;
import gov.openoed.deadline.NoticeDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Dev accounts and a handful of filings that exercise every queue. Idempotent once applications exist. */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataLoader implements ApplicationRunner {

	private final UserRepository users;
	private final PasswordEncoder passwords;
	private final ApplicationRepository applications;
	private final FilingService filings;
	private final OfficeActionService officeActions;
	private final ReviewService reviews;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		UserAccount applicant = user("applicant@openoed.local", "Avery Applicant", Role.APPLICANT, null, "Applicant#12345");
		UserAccount examiner = user("examiner@openoed.local", "Eden Examiner", Role.EXAMINER, "L2", "Examiner#12345");
		user("examiner2@openoed.local", "Noor Examiner", Role.EXAMINER, "L3", "Examiner#12345");
		UserAccount oed = user("oed@openoed.local", "Owen OED", Role.OED, null, "Oed#12345");
		user("ig@openoed.local", "Ira Inspector", Role.IG, null, "Inspector#12345");
		user("admin@openoed.local", "Ada Admin", Role.ADMIN, null, "Admin#12345");
		if (applications.count() > 0) return;

		byte[] clean = picture(false, false, true);
		byte[] cloned = picture(true, false, false);
		ApplicationDetail coffee = file(applicant, "NORTHWIND COFFEE", 30, "Roasted coffee beans and tea",
				"Northwind Coffee", "18 Dock Street, Portland, ME 04101", "ada@northwind.example", null, null, clean);
		file(applicant, "NORTHWIND SUPPLY", 30, "Roasted coffee beans and tea leaves",
				"Northwind Supply LLC", "19 Dock Street, Portland, ME 04101", "ada@northwind.example", null, null, clean);
		ApplicationDetail lumen = file(applicant, "AURORA LUMEN", 3,
				"Elevate your everyday with our cutting-edge, seamless, unparalleled skincare elixir designed to transform your routine and unlock radiant confidence.",
				"Global Prime Holdings LLC", "1234 Market Street, Suite 400, Wilmington, DE 19801",
				"filings@prime.example", "Morgan Shell", "99999", cloned);
		file(applicant, "NIKEO", 25, "Shirts, jackets and footwear apparel",
				"Streetwear LLC", "1234 Market Street, Suite 400, Wilmington, DE 19801",
				"filings@prime.example", null, null, picture(false, false, false));
		ApplicationDetail harbor = file(applicant, "HARBOR SOAP", 3, "Bar soap and hand lotion for personal care",
				"Harbor Soap Co.", "18 Wharf Lane, Bath, ME 04530", "hello@harborsoap.example", null, null, picture(false, false, true));
		ApplicationDetail forge = file(applicant, "PIXELFORGE", 9, "Downloadable design software and a computer application",
				"Pixel Forge", "400 Pine Street, Seattle, WA 98101", "hi@pixelforge.example", null, null, picture(false, false, true));
		ApplicationDetail cedar = file(applicant, "CEDAR AND CO", 21, "Kitchen mugs, kettles and cookware",
				"Cedar and Co", "9 Cedar Ave, Burlington, VT 05401", "hi@cedar.example", null, null, picture(false, false, true));

		officeActions.issue(examiner, harbor.id(), "NONFINAL",
				"The specimen does not show the mark used on the goods. Submit a substitute specimen or amend to intent-to-use.");
		ApplicationDetail issued = officeActions.issue(examiner, forge.id(), "NONFINAL",
				"Identification of goods is indefinite. Respond within the statutory period.");
		var outcome = officeActions.evaluate(issued.officeActions().get(0).id());
		if (outcome.notice() != null) {
			officeActions.recordNotice(new NoticeDraft(outcome.notice().officeActionId(), outcome.notice().applicationId(),
					outcome.notice().toEmail(), outcome.notice().subject(), outcome.notice().text(), "ses-seed-pixelforge"));
		}
		ApplicationDetail responded = officeActions.issue(examiner, cedar.id(), "NONFINAL", "Please clarify the goods in Class 21.");
		officeActions.respond(applicant, responded.officeActions().get(0).id(),
				"The goods are stainless-steel kettles and ceramic mugs sold in our Burlington shop.");
		reviews.decide(oed, lumen.id(), "ASSIGN", "Specimen clone plus a suspended attorney. Holding for a second look.");
		log.info("Seeded OpenOED demo filings (coffee {}, lumen {})", coffee.serialNumber(), lumen.serialNumber());
	}

	private ApplicationDetail file(UserAccount owner, String mark, int cls, String goods, String name, String address,
			String email, String attorney, String bar, byte[] specimen) {
		return filings.file(owner, new FilingService.CreateCommand(mark, "STANDARD", cls, goods, name, address, email, attorney, bar, specimen, "image/jpeg"));
	}

	private UserAccount user(String email, String name, Role role, String artUnit, String password) {
		return users.findByEmailIgnoreCase(email).orElseGet(() -> {
			UserAccount user = new UserAccount();
			user.setId(UUID.randomUUID());
			user.setEmail(email);
			user.setDisplayName(name);
			user.setRole(role);
			user.setArtUnit(artUnit);
			user.setPasswordHash(passwords.encode(password));
			user.setEnabled(true);
			user.setCreatedAt(java.time.Instant.now());
			return users.save(user);
		});
	}

	static byte[] picture(boolean clone, boolean overlay, boolean exif) {
		BufferedImage image = new BufferedImage(160, 160, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < 160; y++) {
			for (int x = 0; x < 160; x++) {
				int n = (x * 13 + y * 7) % 50;
				image.setRGB(x, y, new Color(30 + n, 80 + (x % 20), 60 + (y % 25)).getRGB());
			}
		}
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(250, 250, 250));
		g.fillRect(24, 24, 40, 40);
		g.dispose();
		if (clone) {
			for (int y = 20; y < 100; y++) for (int x = 20; x < 70; x++) image.setRGB(x + 60, y, image.getRGB(x, y));
		}
		if (overlay) {
			Graphics2D o = image.createGraphics();
			o.setColor(new Color(20, 20, 180));
			o.fillRect(0, 0, 160, 100);
			o.dispose();
		}
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", out);
			byte[] jpeg = out.toByteArray();
			if (!exif) return jpeg;
			byte[] header = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1, 0x00, 0x08, 'E', 'x', 'i', 'f', 0, 0};
			byte[] all = new byte[header.length + jpeg.length - 2];
			System.arraycopy(header, 0, all, 0, header.length);
			System.arraycopy(jpeg, 2, all, header.length, jpeg.length - 2);
			return all;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
