package choir.api.production;

public enum MultiOutputRegistrationResult {
	ACCEPTED,
	IDEMPOTENT,
	REJECTED_CONFLICT,
	REJECTED_TARGET_CONFLICT
}
