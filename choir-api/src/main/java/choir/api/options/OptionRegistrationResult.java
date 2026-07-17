package choir.api.options;

public enum OptionRegistrationResult {
	ACCEPTED,
	IDEMPOTENT,
	REJECTED_INVALID,
	REJECTED_CONFLICT
}
