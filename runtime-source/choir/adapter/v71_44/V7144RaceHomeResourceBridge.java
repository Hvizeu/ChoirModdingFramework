package choir.adapter.v71_44;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import choir.api.race.RaceHomeResourceRequirement;
import choir.api.race.RaceMissingTargetPolicy;
import choir.internal.ChoirDiagnostics;
import choir.internal.race.RaceHomeResourceRegistry;
import choir.internal.race.RaceHomeResourceRegistry.TargetPlan;
import choir.internal.race.RaceRegistry;
import init.race.RACES;
import init.race.Race;
import init.race.home.RaceHomeClass;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import init.resources.RES_AMOUNT;
import init.type.HCLASS;
import init.type.HCLASSES;

/** Exact-V71.44 materializer for additive household-resource requirements. */
final class V7144RaceHomeResourceBridge {
	private static final int MAX_DISTINCT_HOME_RESOURCES = 8;
	private static final Map<String, Field> fields = new HashMap<String, Field>();
	private static boolean compatible;
	private static long lastGeneration;
	private static int lastRegistryIdentity;

	private V7144RaceHomeResourceBridge() { }

	static void initialize() {
		V7144RaceTargetFingerprint.Result result = V7144RaceTargetFingerprint.verify();
		compatible = result.matches;
		try {
			field(RaceHomeClass.class, "amounts");
			field(RaceHomeClass.class, "ramounts");
			field(RaceHomeClass.class, "amountTotal");
		} catch (Exception failure) {
			compatible = false;
			ChoirDiagnostics.error("RACE home-resource-reflection-check-failed cause="
					+ failure.getClass().getSimpleName() + ": " + failure.getMessage());
		}
		RaceHomeResourceRegistry.adapterReady(compatible);
		ChoirDiagnostics.info("RACE home-resource-compatibility matched=" + compatible
				+ " gameJar=" + result.jar + " targets=" + result.actual);
	}

	static synchronized void materialize(long runtimeGeneration) {
		List<TargetPlan> plan = RaceHomeResourceRegistry.plan();
		int registryIdentity = System.identityHashCode(RACES.all());
		if (lastGeneration == runtimeGeneration && lastRegistryIdentity == registryIdentity)
			throw new IllegalStateException("Race household resources materialized twice for runtime generation " + runtimeGeneration);
		lastGeneration = runtimeGeneration;
		lastRegistryIdentity = registryIdentity;
		if (!compatible)
			throw new IllegalStateException("The fingerprint-gated V71.44 household-resource adapter is unavailable.");

		Map<String, Race> races = races();
		LinkedHashMap<String, HomePlan> homes = new LinkedHashMap<String, HomePlan>();
		ArrayList<String> targets = new ArrayList<String>();
		for (TargetPlan target : plan) {
			Race race = races.get(target.raceId);
			RESOURCE resource = resource(target.resourceId);
			HCLASS residentClass = hclass(target.residentClass.name());
			if (race == null || resource == null || residentClass == null) {
				if (required(target))
					throw new IllegalStateException("Required household resource target is missing: " + target.targetId);
				ChoirDiagnostics.info("RACE home-resource-skipped target=" + target.targetId + " reason=target-missing");
				continue;
			}
			RaceHomeClass home = race.home().clas(residentClass);
			int[] rawAmounts = (int[]) get(field(RaceHomeClass.class, "ramounts"), home);
			int effective = RaceHomeResourceRegistry.resolve(target, rawAmounts[resource.index()]).maxAmount;
			String homeId = target.raceId + '\u0000' + target.residentClass.name();
			homes.computeIfAbsent(homeId, ignored -> new HomePlan(target.raceId,
					target.residentClass.name(), home)).requirements.add(new EffectiveRequirement(target, resource, effective));
		}

		for (HomePlan home : homes.values()) {
			apply(home);
			for (EffectiveRequirement requirement : home.requirements) {
				targets.add(requirement.target.targetId);
				ChoirDiagnostics.info("RACE home-resource-materialized runtime.generation=" + runtimeGeneration
						+ " target=" + requirement.target.targetId + " effective.max=" + requirement.amount
						+ " contributors=" + contributors(requirement.target));
			}
		}
		RaceRegistry.homeResourcesMaterialized(runtimeGeneration, targets);
		ChoirDiagnostics.info("RACE home-resource-registry runtime.generation=" + runtimeGeneration
				+ " registryIdentity=" + registryIdentity + " targets=" + plan.size()
				+ " materialized=" + targets.size() + " state=CONFIRMED");
	}

	private static void apply(HomePlan plan) {
		List<RES_AMOUNT> base = new ArrayList<RES_AMOUNT>();
		for (RES_AMOUNT amount : plan.home.resources()) base.add(amount);
		Map<RESOURCE, EffectiveRequirement> requested = new LinkedHashMap<RESOURCE, EffectiveRequirement>();
		for (EffectiveRequirement requirement : plan.requirements)
			requested.put(requirement.resource, requirement);

		ArrayList<EffectiveRequirement> additions = new ArrayList<EffectiveRequirement>();
		for (EffectiveRequirement requirement : plan.requirements) {
			boolean present = false;
			for (RES_AMOUNT amount : base) if (amount.resource() == requirement.resource) { present = true; break; }
			if (!present) additions.add(requirement);
		}
		additions.sort((a, b) -> a.target.resourceId.compareTo(b.target.resourceId));
		if (base.size() + additions.size() > MAX_DISTINCT_HOME_RESOURCES)
			throw new IllegalStateException("Household resource limit exceeded race=" + plan.raceId
					+ " class=" + plan.residentClass + " existing=" + base.size()
					+ " additions=" + additions.size() + " max=" + MAX_DISTINCT_HOME_RESOURCES);

		snake2d.util.sets.ArrayList<RES_AMOUNT> replacement =
				new snake2d.util.sets.ArrayList<RES_AMOUNT>(base.size() + additions.size());
		int[] rawAmounts = (int[]) get(field(RaceHomeClass.class, "ramounts"), plan.home);
		int total = 0;
		for (RES_AMOUNT amount : base) {
			EffectiveRequirement requirement = requested.get(amount.resource());
			int effective = requirement == null ? amount.amount() : Math.max(amount.amount(), requirement.amount);
			rawAmounts[amount.resource().index()] = effective;
			replacement.add(new RES_AMOUNT.Abs(amount.resource(), effective));
			total += effective;
		}
		for (EffectiveRequirement requirement : additions) {
			rawAmounts[requirement.resource.index()] = requirement.amount;
			replacement.add(new RES_AMOUNT.Abs(requirement.resource, requirement.amount));
			total += requirement.amount;
		}
		set(field(RaceHomeClass.class, "amounts"), plan.home, replacement);
		setInt(field(RaceHomeClass.class, "amountTotal"), plan.home, total);

		if (plan.home.resources().size() != replacement.size())
			throw new IllegalStateException("Household resource verification failed for " + plan.raceId + '/' + plan.residentClass);
		for (int i = 0; i < base.size(); i++)
			if (plan.home.resources().get(i).resource() != base.get(i).resource())
				throw new IllegalStateException("Existing household resource order changed for " + plan.raceId + '/' + plan.residentClass);
	}

	private static boolean required(TargetPlan plan) {
		for (RaceHomeResourceRequirement requirement : plan.requirements)
			if (requirement.missingTargetPolicy() == RaceMissingTargetPolicy.FAIL) return true;
		return false;
	}
	private static List<String> contributors(TargetPlan plan) {
		ArrayList<String> result = new ArrayList<String>();
		for (RaceHomeResourceRequirement requirement : plan.requirements)
			result.add(requirement.providerId() + '/' + requirement.requirementId() + "=" + requirement.maxAmount());
		return List.copyOf(result);
	}
	private static Map<String, Race> races() {
		HashMap<String, Race> result = new HashMap<String, Race>();
		for (Race race : RACES.all())
			if (result.put(race.key(), race) != null) throw new IllegalStateException("Duplicate live race ID: " + race.key());
		return result;
	}
	private static RESOURCE resource(String id) {
		for (RESOURCE resource : RESOURCES.ALL()) {
			boolean fixedAlias = RESOURCES.map().tryGet(V7144ResourceStableIds.publicAliasCandidate(resource.key())) == resource;
			if (V7144ResourceStableIds.canonical(resource.key(), fixedAlias).equals(id)) return resource;
		}
		return null;
	}
	private static HCLASS hclass(String id) {
		for (HCLASS value : HCLASSES.ALL()) if (value.key().equals(id)) return value;
		return null;
	}
	private static Field field(Class<?> type, String name) {
		String key = type.getName() + '#' + name;
		Field old = fields.get(key);
		if (old != null) return old;
		try {
			Field value = type.getDeclaredField(name);
			value.setAccessible(true);
			fields.put(key, value);
			return value;
		} catch (Exception failure) {
			throw new IllegalStateException("Required exact-V71.44 field is unavailable: " + key, failure);
		}
	}
	private static Object get(Field field, Object owner) {
		try { return field.get(owner); }
		catch (Exception failure) { throw new IllegalStateException("Could not read " + field, failure); }
	}
	private static void set(Field field, Object owner, Object value) {
		try { field.set(owner, value); }
		catch (Exception failure) { throw new IllegalStateException("Could not write " + field, failure); }
	}
	private static void setInt(Field field, Object owner, int value) {
		try { field.setInt(owner, value); }
		catch (Exception failure) { throw new IllegalStateException("Could not write " + field, failure); }
	}

	static synchronized void disposed() {
		lastGeneration = 0;
		lastRegistryIdentity = 0;
	}

	private static final class HomePlan {
		final String raceId;
		final String residentClass;
		final RaceHomeClass home;
		final ArrayList<EffectiveRequirement> requirements = new ArrayList<EffectiveRequirement>();
		HomePlan(String raceId, String residentClass, RaceHomeClass home) {
			this.raceId = raceId;
			this.residentClass = residentClass;
			this.home = home;
		}
	}
	private static final class EffectiveRequirement {
		final TargetPlan target;
		final RESOURCE resource;
		final int amount;
		EffectiveRequirement(TargetPlan target, RESOURCE resource, int amount) {
			this.target = target;
			this.resource = resource;
			this.amount = amount;
		}
	}
}
