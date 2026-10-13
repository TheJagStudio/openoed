package gov.openoed.api.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import tools.jackson.databind.json.JsonMapper;
import gov.openoed.api.config.AppProperties;
import gov.openoed.api.office.NoticeRepository;
import gov.openoed.api.office.OfficeAction;
import gov.openoed.api.office.OfficeActionRepository;
import gov.openoed.api.office.ResponseRepository;
import gov.openoed.api.outbox.DomainOutboxRepository;
import gov.openoed.api.platform.PlatformClient;
import gov.openoed.api.trademark.ApplicationStatus;
import gov.openoed.api.trademark.TrademarkApplication;
import gov.openoed.api.trademark.service.FilingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfficeActionServiceTest {

	@Mock OfficeActionRepository officeActions;
	@Mock ResponseRepository responses;
	@Mock NoticeRepository notices;
	@Mock DomainOutboxRepository outbox;
	@Mock FilingService filings;
	@Mock PlatformClient platform;

	OfficeActionService service;
	OfficeAction action;
	TrademarkApplication app;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties(
				new AppProperties.Jwt("iss", java.time.Duration.ofMinutes(15), java.time.Duration.ofDays(1), "k"),
				new AppProperties.Platform("http://localhost", "tok"),
				new AppProperties.Claude(false, "", "m", "http://localhost"),
				new AppProperties.Deadlines(6, java.time.Duration.ofMinutes(30), true),
				new AppProperties.Seed(false),
				new AppProperties.Storage("s"));
		service = new OfficeActionService(officeActions, responses, notices, outbox, filings, platform, props,
				JsonMapper.builder().build(), Clock.fixed(Instant.parse("2026-10-12T15:00:00Z"), ZoneOffset.UTC));
		action = new OfficeAction();
		action.setId(UUID.randomUUID());
		action.setApplicationId(UUID.randomUUID());
		action.setStatus("OPEN");
		action.setStatutoryDueAt(Instant.parse("2027-04-12T15:00:00Z"));
		app = new TrademarkApplication();
		app.setId(action.getApplicationId());
		app.setSerialNumber("97000001");
		app.setMark("HARBOR");
		app.setOwnerEmail("ada@example.com");
		app.setStatus(ApplicationStatus.OFFICE_ACTION);
	}

	@Test
	void noResponseAbandonsAndDraftsANotice() {
		when(officeActions.lock(action.getId())).thenReturn(Optional.of(action));
		when(responses.existsByOfficeActionId(action.getId())).thenReturn(false);
		when(notices.findByOfficeActionId(action.getId())).thenReturn(Optional.empty());
		when(filings.loadVisible(any(), any())).thenReturn(app);
		var outcome = service.evaluate(action.getId());
		assertThat(outcome.outcome()).isEqualTo("ABANDONED");
		assertThat(outcome.notice().toEmail()).isEqualTo("ada@example.com");
		assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ABANDONED);
		assertThat(action.getStatus()).isEqualTo("EXPIRED");
	}

	@Test
	void aResponsePreventsAbandonment() {
		when(officeActions.lock(action.getId())).thenReturn(Optional.of(action));
		when(responses.existsByOfficeActionId(action.getId())).thenReturn(true);
		assertThat(service.evaluate(action.getId()).outcome()).isEqualTo("ALREADY_RESPONDED");
		assertThat(app.getStatus()).isEqualTo(ApplicationStatus.OFFICE_ACTION);
	}
}
