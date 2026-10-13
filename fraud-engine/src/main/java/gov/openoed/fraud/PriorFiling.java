package gov.openoed.fraud;

/** A previously filed application used for velocity and specimen-reuse checks. */
public record PriorFiling(String serialNumber, String ownerEmail, String address, String specimenSha256) {
}
