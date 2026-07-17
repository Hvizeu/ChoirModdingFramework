package choir.api.lifecycle;

public final class LifecycleEvent<C> {
	private final String id;
	private final Class<C> contextType;
	private final boolean replayLatest;
	public LifecycleEvent(String id, Class<C> contextType, boolean replayLatest) {
		if (id == null || !id.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid lifecycle event ID: " + id);
		if (contextType == null) throw new IllegalArgumentException("Context type is required.");
		this.id = id; this.contextType = contextType; this.replayLatest = replayLatest;
	}
	public String id() { return id; }
	public Class<C> contextType() { return contextType; }
	public boolean replayLatest() { return replayLatest; }
}
