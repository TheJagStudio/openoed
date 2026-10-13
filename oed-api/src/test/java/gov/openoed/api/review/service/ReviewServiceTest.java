package gov.openoed.api.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import gov.openoed.api.auth.Role;
import gov.openoed.api.auth.UserAccount;
import gov.openoed.api.common.ForbiddenException;
import gov.openoed.api.review.ReviewItem;
import gov.openoed.api.review.ReviewRepository;
import gov.openoed.api.trademark.ApplicationStatus;
import gov.openoed.api.trademark.TrademarkApplication;
import gov.openoed.api.trademark.service.FilingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

	@Mock ReviewRepository reviews;
	@Mock FilingService filings;

	@Test
	void confirmRefusesTheApplication() {
		ReviewService service = service();
		UserAccount oed = user(Role.OED);
		UUID id = UUID.randomUUID();
		ReviewItem item = new ReviewItem();
		item.setStatus("OPEN");
		item.setApplicationId(id);
		TrademarkApplication app = new TrademarkApplication();
		app.setId(id);
		app.setStatus(ApplicationStatus.SUBMITTED);
		when(reviews.findByApplicationId(id)).thenReturn(Optional.of(item));
		when(filings.loadVisible(oed, id)).thenReturn(app);
		service.decide(oed, id, "confirm", "shell pattern");
		assertThat(item.getStatus()).isEqualTo("CONFIRMED");
		assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REFUSED);
	}

	@Test
	void applicantsCannotOpenTheQueue() {
		assertThatThrownBy(() -> service().queue(user(Role.APPLICANT), "OPEN")).isInstanceOf(ForbiddenException.class);
	}

	private ReviewService service() {
		return new ReviewService(reviews, filings, Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC));
	}

	private static UserAccount user(Role role) {
		UserAccount user = new UserAccount();
		user.setId(UUID.randomUUID());
		user.setRole(role);
		return user;
	}
}
