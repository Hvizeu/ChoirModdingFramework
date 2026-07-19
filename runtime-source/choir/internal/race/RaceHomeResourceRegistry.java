package choir.internal.race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import choir.api.race.RaceHomeResourceRequirement;
import choir.api.race.RaceRegistrationResult;
import choir.internal.ChoirDiagnostics;
import choir.internal.platform.PlatformRuntime;

/** Process-retained, game-type-free household-resource requirements. */
public final class RaceHomeResourceRegistry {
	private static final Map<String, RaceHomeResourceRequirement> requirements =
			new TreeMap<String, RaceHomeResourceRequirement>();
	private static boolean adapterReady;

	private RaceHomeResourceRegistry() { }

	public static synchronized RaceRegistrationResult register(RaceHomeResourceRequirement requirement) {
		if (requirement == null) throw new IllegalArgumentException("A household resource requirement is required.");
		String identity = identity(requirement);
		RaceHomeResourceRequirement old = requirements.get(identity);
		RaceRegistrationResult result;
		if (old == null) {
			requirements.put(identity, requirement);
			result = RaceRegistrationResult.ACCEPTED;
		} else {
			result = equivalent(old, requirement)
					? RaceRegistrationResult.IDEMPOTENT : RaceRegistrationResult.REJECTED_CONFLICT;
		}
		ChoirDiagnostics.info("RACE home-resource-registration identity="
				+ identity.replace('\u0000', '/') + " target=" + targetId(requirement)
				+ " max=" + requirement.maxAmount() + " result=" + result);
		return result;
	}

	public static synchronized List<TargetPlan> plan() {
		TreeMap<String, ArrayList<RaceHomeResourceRequirement>> grouped =
				new TreeMap<String, ArrayList<RaceHomeResourceRequirement>>();
		for (RaceHomeResourceRequirement requirement : requirements.values()) {
			requireProvider(requirement.providerId());
			grouped.computeIfAbsent(targetId(requirement), ignored ->
					new ArrayList<RaceHomeResourceRequirement>()).add(requirement);
		}
		ArrayList<TargetPlan> result = new ArrayList<TargetPlan>();
		for (Map.Entry<String, ArrayList<RaceHomeResourceRequirement>> entry : grouped.entrySet()) {
			entry.getValue().sort(order());
			RaceHomeResourceRequirement first = entry.getValue().get(0);
			result.add(new TargetPlan(entry.getKey(), first.raceId(), first.residentClass(),
					first.resourceId(), entry.getValue()));
		}
		return List.copyOf(result);
	}

	public static Resolution resolve(TargetPlan plan, int baseAmount) {
		if (plan == null) throw new IllegalArgumentException("A household resource target plan is required.");
		int effective = baseAmount;
		for (RaceHomeResourceRequirement requirement : plan.requirements)
			effective = Math.max(effective, requirement.maxAmount());
		return new Resolution(plan.targetId, effective);
	}

	public static synchronized void adapterReady(boolean ready) { adapterReady = ready; }
	public static synchronized boolean capabilityReady() { return adapterReady; }
	static synchronized void resetForTests() { requirements.clear(); adapterReady = false; }

	private static String identity(RaceHomeResourceRequirement requirement) {
		return requirement.providerId() + '\u0000' + requirement.requirementId();
	}
	private static String targetId(RaceHomeResourceRequirement requirement) {
		return "choir.race.home-resource:" + requirement.raceId() + ':'
				+ requirement.residentClass() + ':' + requirement.resourceId();
	}
	private static Comparator<RaceHomeResourceRequirement> order() {
		return Comparator.comparing(RaceHomeResourceRequirement::providerId)
				.thenComparing(RaceHomeResourceRequirement::requirementId);
	}
	private static void requireProvider(String providerId) {
		if (!PlatformRuntime.isActive(providerId))
			throw new IllegalStateException("Household resource provider is not active in the resolved platform graph: " + providerId);
	}
	private static boolean equivalent(RaceHomeResourceRequirement a, RaceHomeResourceRequirement b) {
		return a.providerId().equals(b.providerId())
				&& a.requirementId().equals(b.requirementId())
				&& a.raceId().equals(b.raceId())
				&& a.residentClass() == b.residentClass()
				&& a.resourceId().equals(b.resourceId())
				&& a.maxAmount() == b.maxAmount()
				&& a.missingTargetPolicy() == b.missingTargetPolicy();
	}

	public static final class TargetPlan {
		public final String targetId;
		public final String raceId;
		public final choir.api.race.RaceHomeResidentClass residentClass;
		public final String resourceId;
		public final List<RaceHomeResourceRequirement> requirements;

		TargetPlan(String targetId, String raceId,
				choir.api.race.RaceHomeResidentClass residentClass, String resourceId,
				List<RaceHomeResourceRequirement> requirements) {
			this.targetId = targetId;
			this.raceId = raceId;
			this.residentClass = residentClass;
			this.resourceId = resourceId;
			this.requirements = List.copyOf(requirements);
		}
	}

	public static final class Resolution {
		public final String targetId;
		public final int maxAmount;
		Resolution(String targetId, int maxAmount) {
			this.targetId = targetId;
			this.maxAmount = maxAmount;
		}
	}
}
