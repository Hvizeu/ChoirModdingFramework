package choir.api.experimental.resources;

/** Typed result for the experimental, presentation-only registration API. */
public enum ResourceDisplayRegistrationResult {
	ACCEPTED,
	IDEMPOTENT,
	REJECTED_CONFLICT
}
