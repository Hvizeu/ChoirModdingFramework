package choir.api.patch;

public final class PatchContribution<T> {
	private final String providerId;
	private final String patchId;
	private final String targetId;
	private final int priority;
	private final T value;
	public PatchContribution(String providerId, String patchId, String targetId, int priority, T value) {
		if (providerId == null || !providerId.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid provider ID: " + providerId);
		if (patchId == null || !patchId.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid patch ID: " + patchId);
		if (targetId == null || targetId.isBlank() || value == null) throw new IllegalArgumentException("Target and value are required.");
		this.providerId = providerId; this.patchId = patchId; this.targetId = targetId; this.priority = priority; this.value = value;
	}
	public String providerId() { return providerId; }
	public String patchId() { return patchId; }
	public String targetId() { return targetId; }
	public int priority() { return priority; }
	public T value() { return value; }
}
