package choir.api.patch;

import java.util.List;

public final class PatchResolution<T> {
	private final String targetId;
	private final T value;
	private final List<PatchContribution<T>> contributions;
	public PatchResolution(String targetId, T value, List<PatchContribution<T>> contributions) {
		this.targetId = targetId; this.value = value; this.contributions = List.copyOf(contributions);
	}
	public String targetId() { return targetId; }
	public T value() { return value; }
	public List<PatchContribution<T>> contributions() { return contributions; }
}
