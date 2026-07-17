package choir.internal.patch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import choir.api.patch.PatchContribution;
import choir.api.patch.PatchRegistrationResult;
import choir.api.patch.PatchResolution;
import choir.api.patch.PatchTarget;
import choir.internal.ChoirDiagnostics;

public final class PatchRegistry {
	private static final Map<String, PatchTarget<?>> targets = new TreeMap<String, PatchTarget<?>>();
	private static final Map<String, PatchContribution<?>> contributions = new TreeMap<String, PatchContribution<?>>();
	private PatchRegistry() { }
	public static synchronized <T> PatchRegistrationResult registerTarget(PatchTarget<T> target) {
		if (target == null) throw new IllegalArgumentException("Patch target is required.");
		PatchTarget<?> old = targets.get(target.targetId());
		if (old == null) { targets.put(target.targetId(), target); return PatchRegistrationResult.ACCEPTED; }
		return equivalentTarget(old, target) ? PatchRegistrationResult.IDEMPOTENT : PatchRegistrationResult.REJECTED_CONFLICT;
	}
	public static synchronized <T> PatchRegistrationResult contribute(PatchContribution<T> contribution) {
		if (contribution == null) throw new IllegalArgumentException("Patch contribution is required.");
		PatchTarget<?> target = targets.get(contribution.targetId());
		if (target == null) return PatchRegistrationResult.REJECTED_UNKNOWN_TARGET;
		if (!target.valueType().isInstance(contribution.value())) return PatchRegistrationResult.REJECTED_TYPE;
		String key = contribution.targetId() + '\u0000' + contribution.providerId() + '\u0000' + contribution.patchId();
		PatchContribution<?> old = contributions.get(key);
		PatchRegistrationResult result;
		if (old == null) { contributions.put(key, contribution); result = PatchRegistrationResult.ACCEPTED; }
		else result = equivalentContribution(old, contribution) ? PatchRegistrationResult.IDEMPOTENT : PatchRegistrationResult.REJECTED_CONFLICT;
		ChoirDiagnostics.info("PATCH contribute provider=" + contribution.providerId() + " patch=" + contribution.patchId()
				+ " target=" + contribution.targetId() + " priority=" + contribution.priority() + " result=" + result);
		return result;
	}
	public static synchronized <T> PatchResolution<T> resolve(String targetId, Class<T> valueType) {
		PatchTarget<?> raw = targets.get(targetId);
		if (raw == null) throw new IllegalArgumentException("Unknown patch target: " + targetId);
		if (raw.valueType() != valueType) throw new IllegalArgumentException("Patch target type mismatch for " + targetId);
		@SuppressWarnings("unchecked") PatchTarget<T> target = (PatchTarget<T>) raw;
		ArrayList<PatchContribution<T>> values = new ArrayList<PatchContribution<T>>();
		for (PatchContribution<?> candidate : contributions.values()) if (candidate.targetId().equals(targetId)) {
			@SuppressWarnings("unchecked") PatchContribution<T> typed = (PatchContribution<T>) candidate; values.add(typed);
		}
		values.sort(Comparator.comparingInt(PatchContribution<T>::priority)
				.thenComparing(PatchContribution<T>::providerId).thenComparing(PatchContribution<T>::patchId));
		T result = target.baseValue();
		for (PatchContribution<T> value : values) result = target.composer().compose(result, value.value());
		return new PatchResolution<T>(targetId, result, values);
	}
	public static synchronized List<String> targetIds() { return List.copyOf(targets.keySet()); }
	static synchronized void resetForTests() { targets.clear(); contributions.clear(); }
	private static boolean equivalentTarget(PatchTarget<?> a, PatchTarget<?> b) {
		return a.ownerId().equals(b.ownerId()) && a.targetId().equals(b.targetId()) && a.valueType() == b.valueType()
				&& a.baseValue().equals(b.baseValue()) && a.composerId().equals(b.composerId());
	}
	private static boolean equivalentContribution(PatchContribution<?> a, PatchContribution<?> b) {
		return a.providerId().equals(b.providerId()) && a.patchId().equals(b.patchId()) && a.targetId().equals(b.targetId())
				&& a.priority() == b.priority() && a.value().equals(b.value());
	}
}
