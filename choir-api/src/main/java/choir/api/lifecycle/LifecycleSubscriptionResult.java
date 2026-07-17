package choir.api.lifecycle;

public enum LifecycleSubscriptionResult {
	ACCEPTED,
	IDEMPOTENT,
	REJECTED_CONFLICT
}
