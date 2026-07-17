package choir.api.patch;

public enum PatchRegistrationResult {
	ACCEPTED,
	IDEMPOTENT,
	REJECTED_CONFLICT,
	REJECTED_UNKNOWN_TARGET,
	REJECTED_TYPE
}
