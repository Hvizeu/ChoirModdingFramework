package choir.api.experimental.resources;

/** Explicit readiness state for non-throwing effective-model queries. */
public enum ResourceDisplayRuntimeState {
	ADAPTER_UNAVAILABLE,
	REGISTRY_NOT_READY,
	NO_REGISTRATIONS,
	MODEL_READY,
	MODEL_STALE,
	DISABLED,
	REBUILD_FAILED
}
