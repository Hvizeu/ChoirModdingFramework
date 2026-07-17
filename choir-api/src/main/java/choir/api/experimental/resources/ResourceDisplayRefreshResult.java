package choir.api.experimental.resources;

/** Refresh requests invalidate state only; this gate performs no UI work. */
public enum ResourceDisplayRefreshResult {
	REQUESTED,
	COALESCED,
	DISABLED
}
