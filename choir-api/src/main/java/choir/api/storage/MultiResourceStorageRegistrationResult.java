package choir.api.storage;

public enum MultiResourceStorageRegistrationResult {
	ACCEPTED,
	IDEMPOTENT,
	REJECTED_CONFLICT,
	REJECTED_TARGET_CONFLICT
}
