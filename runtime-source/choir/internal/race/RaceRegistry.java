package choir.internal.race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import choir.api.patch.PatchComposers;
import choir.api.patch.PatchContribution;
import choir.api.patch.PatchRegistrationResult;
import choir.api.patch.PatchResolution;
import choir.api.patch.PatchTarget;
import choir.api.race.RaceBoostPatch;
import choir.api.race.RaceDeclaration;
import choir.api.race.RacePreferenceKind;
import choir.api.race.RacePreferenceOperation;
import choir.api.race.RacePreferencePatch;
import choir.api.race.RaceRegistrationResult;
import choir.api.race.RaceRuntimeSnapshot;
import choir.internal.ChoirDiagnostics;
import choir.internal.patch.PatchRegistry;
import choir.internal.platform.CorePlatformRuntime;
import choir.internal.platform.PlatformRuntime;

/** Process descriptors plus per-live-registry materialization evidence. No game types cross this boundary. */
public final class RaceRegistry {
	private static final Map<String, RaceDeclaration> declarations = new TreeMap<String, RaceDeclaration>();
	private static final Map<String, RaceBoostPatch> patches = new TreeMap<String, RaceBoostPatch>();
	private static final Map<String, RacePreferencePatch> preferencePatches = new TreeMap<String, RacePreferencePatch>();
	private static final Map<String, String> patchTargets = new TreeMap<String, String>();
	private static RaceRuntimeSnapshot runtime = new RaceRuntimeSnapshot(0, 0, List.of(), List.of());
	private static boolean preferenceAdapterReady;
	private RaceRegistry() { }
	public static synchronized RaceRegistrationResult declare(RaceDeclaration declaration) {
		if (declaration == null) throw new IllegalArgumentException("Race declaration is required.");
		RaceDeclaration old = declarations.get(declaration.raceId());
		RaceRegistrationResult result;
		if (old == null) { declarations.put(declaration.raceId(), declaration); result = RaceRegistrationResult.ACCEPTED; }
		else if (equivalent(old, declaration)) result = RaceRegistrationResult.IDEMPOTENT;
		else result = RaceRegistrationResult.REJECTED_CONFLICT;
		ChoirDiagnostics.info("RACE declaration provider=" + declaration.providerId() + " race=" + declaration.raceId() + " result=" + result);
		return result;
	}
	public static synchronized RaceRegistrationResult patch(RaceBoostPatch patch) {
		if (patch == null) throw new IllegalArgumentException("Race patch is required.");
		String identity = patch.providerId() + '\u0000' + patch.patchId();
		RaceBoostPatch old = patches.get(identity);
		if (old != null) return equivalent(old, patch) ? RaceRegistrationResult.IDEMPOTENT : RaceRegistrationResult.REJECTED_CONFLICT;
		String targetId = targetId(patch.raceId(), patch.boostableId());
		PatchRegistrationResult target = PatchRegistry.registerTarget(new PatchTarget<Double>("choir.framework", targetId,
				Double.class, Double.valueOf(1.0), PatchComposers.DOUBLE_MULTIPLY));
		if (target != PatchRegistrationResult.ACCEPTED && target != PatchRegistrationResult.IDEMPOTENT) return RaceRegistrationResult.REJECTED_CONFLICT;
		PatchRegistrationResult contribution = PatchRegistry.contribute(new PatchContribution<Double>(patch.providerId(), patch.patchId(),
				targetId, patch.priority(), Double.valueOf(patch.multiplier())));
		if (contribution != PatchRegistrationResult.ACCEPTED && contribution != PatchRegistrationResult.IDEMPOTENT) return RaceRegistrationResult.REJECTED_CONFLICT;
		patches.put(identity, patch); patchTargets.put(targetId, targetId);
		ChoirDiagnostics.info("RACE patch provider=" + patch.providerId() + " patch=" + patch.patchId() + " race=" + patch.raceId()
				+ " boostable=" + patch.boostableId() + " multiplier=" + patch.multiplier() + " result=ACCEPTED");
		return RaceRegistrationResult.ACCEPTED;
	}
	public static synchronized RaceRegistrationResult patchPreference(RacePreferencePatch patch) {
		if (patch == null) throw new IllegalArgumentException("Race preference patch is required.");
		String identity = patch.providerId() + '\u0000' + patch.patchId();
		RacePreferencePatch old = preferencePatches.get(identity);
		RaceRegistrationResult result;
		if (old == null) {
			preferencePatches.put(identity, patch);
			result = RaceRegistrationResult.ACCEPTED;
		} else result = equivalent(old, patch) ? RaceRegistrationResult.IDEMPOTENT : RaceRegistrationResult.REJECTED_CONFLICT;
		ChoirDiagnostics.info("RACE preference-patch provider=" + patch.providerId() + " patch=" + patch.patchId()
				+ " race=" + patch.raceId() + " kind=" + patch.kind() + " operation=" + patch.operation()
				+ " priority=" + patch.priority() + " resources=" + patch.resourceIds() + " result=" + result);
		return result;
	}
	public static synchronized CyclePlan cyclePlan(long cycleId) {
		for (RaceDeclaration declaration : declarations.values()) requireProvider(declaration.providerId());
		for (RaceBoostPatch patch : patches.values()) requireProvider(patch.providerId());
		ArrayList<PatchPlan> plans = new ArrayList<PatchPlan>();
		for (String targetId : patchTargets.keySet()) {
			PatchResolution<Double> resolution = PatchRegistry.resolve(targetId, Double.class);
			String suffix = targetId.substring("choir.race.boost-multiplier:".length());
			int split = suffix.indexOf(':');
			plans.add(new PatchPlan(targetId, suffix.substring(0, split), suffix.substring(split + 1), resolution.value().doubleValue(), resolution.contributions()));
		}
		return new CyclePlan(cycleId, new ArrayList<RaceDeclaration>(declarations.values()), plans);
	}
	public static synchronized void materialized(long cycleId, List<String> resolvedDeclarations, List<String> targets) {
		runtime = new RaceRuntimeSnapshot(cycleId, CorePlatformRuntime.runtimeGeneration(), resolvedDeclarations, targets,
				runtime.materializedPreferenceTargets());
		ChoirDiagnostics.info("RACE materialization cycle=" + cycleId + " declarations=" + resolvedDeclarations.size() + " patchTargets=" + targets.size());
	}
	public static synchronized List<PreferenceTargetPlan> preferencePlans() {
		TreeMap<String, ArrayList<RacePreferencePatch>> grouped = new TreeMap<String, ArrayList<RacePreferencePatch>>();
		for (RacePreferencePatch patch : preferencePatches.values()) {
			requireProvider(patch.providerId());
			String targetId = preferenceTargetId(patch.raceId(), patch.kind());
			grouped.computeIfAbsent(targetId, ignored -> new ArrayList<RacePreferencePatch>()).add(patch);
		}
		ArrayList<PreferenceTargetPlan> result = new ArrayList<PreferenceTargetPlan>();
		for (Map.Entry<String, ArrayList<RacePreferencePatch>> entry : grouped.entrySet()) {
			ArrayList<RacePreferencePatch> values = entry.getValue();
			values.sort(preferenceOrder());
			RacePreferencePatch first = values.get(0);
			result.add(new PreferenceTargetPlan(entry.getKey(), first.raceId(), first.kind(), values));
		}
		return List.copyOf(result);
	}
	public static PreferenceResolution resolvePreference(PreferenceTargetPlan plan, List<String> baseResourceIds) {
		if (plan == null || baseResourceIds == null) throw new IllegalArgumentException("Preference plan and base values are required.");
		LinkedHashSet<String> effective = new LinkedHashSet<String>(baseResourceIds);
		TreeMap<Integer, ArrayList<RacePreferencePatch>> byPriority = new TreeMap<Integer, ArrayList<RacePreferencePatch>>();
		for (RacePreferencePatch patch : plan.patches()) byPriority.computeIfAbsent(patch.priority(), ignored -> new ArrayList<RacePreferencePatch>()).add(patch);
		ArrayList<String> diagnostics = new ArrayList<String>();
		for (Map.Entry<Integer, ArrayList<RacePreferencePatch>> level : byPriority.entrySet()) {
			ArrayList<RacePreferencePatch> replacements = new ArrayList<RacePreferencePatch>();
			TreeMap<String, TreeSet<RacePreferenceOperation>> operations = new TreeMap<String, TreeSet<RacePreferenceOperation>>();
			for (RacePreferencePatch patch : level.getValue()) {
				if (patch.operation() == RacePreferenceOperation.REPLACE) replacements.add(patch);
				for (String resource : patch.resourceIds()) operations.computeIfAbsent(resource,
						ignored -> new TreeSet<RacePreferenceOperation>()).add(patch.operation());
			}
			if (replacements.size() > 1) throw new IllegalStateException("Conflicting race preference replacements target="
					+ plan.targetId() + " priority=" + level.getKey() + " contributors=" + identities(replacements));
			for (Map.Entry<String, TreeSet<RacePreferenceOperation>> operation : operations.entrySet()) {
				if (operation.getValue().contains(RacePreferenceOperation.ADD) && operation.getValue().contains(RacePreferenceOperation.REMOVE))
					throw new IllegalStateException("Conflicting race preference add/remove target=" + plan.targetId()
							+ " priority=" + level.getKey() + " resource=" + operation.getKey());
			}
			if (!replacements.isEmpty()) {
				effective.clear();
				effective.addAll(replacements.get(0).resourceIds());
			}
			TreeSet<String> removes = new TreeSet<String>();
			TreeSet<String> adds = new TreeSet<String>();
			for (RacePreferencePatch patch : level.getValue()) {
				if (patch.operation() == RacePreferenceOperation.REMOVE) removes.addAll(patch.resourceIds());
				if (patch.operation() == RacePreferenceOperation.ADD) adds.addAll(patch.resourceIds());
			}
			effective.removeAll(removes);
			effective.addAll(adds);
			diagnostics.add("priority=" + level.getKey() + " replace=" + identities(replacements)
					+ " remove=" + removes + " add=" + adds);
		}
		if (effective.isEmpty()) throw new IllegalStateException("Race preference composition produced an empty required collection: " + plan.targetId());
		return new PreferenceResolution(plan.targetId(), List.copyOf(effective), plan.patches(), diagnostics);
	}
	public static synchronized void preferencesMaterialized(long runtimeGeneration, List<String> targets) {
		runtime = new RaceRuntimeSnapshot(runtime.cycleId(), runtimeGeneration, runtime.resolvedDeclarations(),
				runtime.materializedPatchTargets(), targets);
		ChoirDiagnostics.info("RACE preference-materialization runtime.generation=" + runtimeGeneration + " targets=" + targets.size());
	}
	public static synchronized void preferenceAdapterReady(boolean ready) { preferenceAdapterReady = ready; }
	public static synchronized boolean preferenceCapabilityReady() { return preferenceAdapterReady; }
	static synchronized void resetPreferencesForTests() { preferencePatches.clear(); preferenceAdapterReady = false; }
	public static synchronized RaceRuntimeSnapshot runtimeSnapshot() { return runtime; }
	public static synchronized void disposeRuntime() { runtime = new RaceRuntimeSnapshot(0, CorePlatformRuntime.runtimeGeneration(), List.of(), List.of(), List.of()); }
	private static void requireProvider(String providerId) { if (!PlatformRuntime.isActive(providerId)) throw new IllegalStateException("Race provider is not active in the resolved platform graph: " + providerId); }
	private static String targetId(String raceId, String boostableId) { return "choir.race.boost-multiplier:" + raceId + ':' + boostableId; }
	private static boolean equivalent(RaceDeclaration a, RaceDeclaration b) { return a.providerId().equals(b.providerId()) && a.raceId().equals(b.raceId()) && a.displayName().equals(b.displayName()); }
	private static boolean equivalent(RaceBoostPatch a, RaceBoostPatch b) { return a.providerId().equals(b.providerId()) && a.patchId().equals(b.patchId()) && a.raceId().equals(b.raceId()) && a.boostableId().equals(b.boostableId()) && a.priority() == b.priority() && Double.compare(a.multiplier(), b.multiplier()) == 0; }
	private static boolean equivalent(RacePreferencePatch a, RacePreferencePatch b) { return a.providerId().equals(b.providerId()) && a.patchId().equals(b.patchId())
			&& a.raceId().equals(b.raceId()) && a.kind() == b.kind() && a.operation() == b.operation()
			&& a.priority() == b.priority() && a.resourceIds().equals(b.resourceIds()); }
	private static String preferenceTargetId(String raceId, RacePreferenceKind kind) { return "choir.race.preference:" + raceId + ':' + kind; }
	private static Comparator<RacePreferencePatch> preferenceOrder() { return Comparator.comparingInt(RacePreferencePatch::priority)
			.thenComparing(RacePreferencePatch::providerId).thenComparing(RacePreferencePatch::patchId); }
	private static List<String> identities(List<RacePreferencePatch> patches) {
		ArrayList<String> values = new ArrayList<String>();
		for (RacePreferencePatch patch : patches) values.add(patch.providerId() + '/' + patch.patchId());
		values.sort(String::compareTo);
		return List.copyOf(values);
	}

	public static final class CyclePlan {
		private final long cycleId; private final List<RaceDeclaration> declarations; private final List<PatchPlan> patches;
		CyclePlan(long cycleId, List<RaceDeclaration> declarations, List<PatchPlan> patches) { this.cycleId = cycleId; this.declarations = List.copyOf(declarations); this.patches = List.copyOf(patches); }
		public long cycleId() { return cycleId; } public List<RaceDeclaration> declarations() { return declarations; } public List<PatchPlan> patches() { return patches; }
	}
	public static final class PatchPlan {
		private final String targetId, raceId, boostableId; private final double multiplier; private final List<PatchContribution<Double>> contributions;
		PatchPlan(String targetId, String raceId, String boostableId, double multiplier, List<PatchContribution<Double>> contributions) {
			this.targetId = targetId; this.raceId = raceId; this.boostableId = boostableId; this.multiplier = multiplier; this.contributions = List.copyOf(contributions);
		}
		public String targetId() { return targetId; } public String raceId() { return raceId; } public String boostableId() { return boostableId; } public double multiplier() { return multiplier; } public List<PatchContribution<Double>> contributions() { return contributions; }
	}
	public static final class PreferenceTargetPlan {
		private final String targetId, raceId; private final RacePreferenceKind kind; private final List<RacePreferencePatch> patches;
		PreferenceTargetPlan(String targetId, String raceId, RacePreferenceKind kind, List<RacePreferencePatch> patches) {
			this.targetId = targetId; this.raceId = raceId; this.kind = kind; this.patches = List.copyOf(patches);
		}
		public String targetId() { return targetId; } public String raceId() { return raceId; }
		public RacePreferenceKind kind() { return kind; } public List<RacePreferencePatch> patches() { return patches; }
	}
	public static final class PreferenceResolution {
		private final String targetId; private final List<String> resourceIds; private final List<RacePreferencePatch> contributions; private final List<String> diagnostics;
		PreferenceResolution(String targetId, List<String> resourceIds, List<RacePreferencePatch> contributions, List<String> diagnostics) {
			this.targetId = targetId; this.resourceIds = List.copyOf(resourceIds); this.contributions = List.copyOf(contributions); this.diagnostics = List.copyOf(diagnostics);
		}
		public String targetId() { return targetId; } public List<String> resourceIds() { return resourceIds; }
		public List<RacePreferencePatch> contributions() { return contributions; } public List<String> diagnostics() { return diagnostics; }
	}
}
