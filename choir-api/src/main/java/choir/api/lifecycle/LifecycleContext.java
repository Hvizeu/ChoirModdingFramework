package choir.api.lifecycle;

/** Version-neutral immutable lifecycle evidence. */
public final class LifecycleContext {
	private final LifecyclePhase phase;
	private final long sequence;
	private final long runtimeGeneration;
	private final String sessionId;
	private final String gameVersion;
	private final String marker;

	public LifecycleContext(LifecyclePhase phase, long sequence, long runtimeGeneration,
			String sessionId, String gameVersion, String marker) {
		this.phase = phase;
		this.sequence = sequence;
		this.runtimeGeneration = runtimeGeneration;
		this.sessionId = sessionId;
		this.gameVersion = gameVersion;
		this.marker = marker;
	}
	public LifecyclePhase phase() { return phase; }
	public long sequence() { return sequence; }
	public long runtimeGeneration() { return runtimeGeneration; }
	public String sessionId() { return sessionId; }
	public String gameVersion() { return gameVersion; }
	public String marker() { return marker; }
}
