package gov.openoed.deadline;

/** SES stand-in. Returns the provider message id. */
public interface Mailer {

	String send(NoticeDraft notice);
}
