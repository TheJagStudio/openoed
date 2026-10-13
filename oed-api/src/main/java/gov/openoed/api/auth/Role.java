package gov.openoed.api.auth;

public enum Role {
	APPLICANT,
	EXAMINER,
	OED,
	IG,
	ADMIN;

	public boolean staff() {
		return this != APPLICANT;
	}
}
