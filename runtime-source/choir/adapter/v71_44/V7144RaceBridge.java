package choir.adapter.v71_44;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import choir.api.patch.PatchContribution;
import choir.api.race.RaceDeclaration;
import choir.api.race.RacePreferenceKind;
import choir.api.race.RacePreferencePatch;
import choir.internal.ChoirDiagnostics;
import choir.internal.race.RaceRegistry;
import choir.internal.race.RaceRegistry.CyclePlan;
import choir.internal.race.RaceRegistry.PatchPlan;
import game.boosting.BOOSTING;
import game.boosting.BSourceInfo;
import game.boosting.BoostSpec;
import game.boosting.Boostable;
import game.boosting.BoosterImp;
import init.race.RACES;
import init.race.Race;
import init.race.RacePreferrence;
import init.resources.RBIT.RBITImp;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import init.resources.ResG;
import snake2d.util.sets.LIST;

/** Exact-v71.44 materializer. No live Race or Boostable crosses the public API. */
final class V7144RaceBridge {
	private static final String RACE_CLASS = "init/race/Race.class";
	private static final String EXPECTED_RACE_SHA256 = "5936e437a0b914545c83e77ffbb67bcd21b1ccdeb7b7ec963d7afa3f150046b2";
	private static final String PREFERENCE_CLASS = "init/race/RacePreferrence.class";
	private static final String EXPECTED_PREFERENCE_SHA256 = "8ce9dfc0f8693feaca48f1cf4b79d8ccc49d930c6dfc29af9f2b37a8d6e8f894";
	private static Field raceBoostMap;
	private static Field foodField, drinkField, foodMaskField, drinkMaskField;
	private static boolean compatible;
	private static boolean preferenceCompatible;
	private static long lastCycle;
	private static int lastRegistryIdentity;
	private static long lastPreferenceRuntimeGeneration;
	private static int lastPreferenceRegistryIdentity;
	private static final Set<String> applied = new HashSet<String>();
	private V7144RaceBridge() { }
	static void initialize() {
		try {
			Enumeration<URL> resources = Race.class.getClassLoader().getResources(RACE_CLASS);
			ArrayList<String> hashes = new ArrayList<String>();
			while (resources.hasMoreElements()) try (InputStream in = resources.nextElement().openStream()) { hashes.add(hex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()))); }
			compatible = hashes.size() == 1 && EXPECTED_RACE_SHA256.equals(hashes.get(0));
			raceBoostMap = Race.class.getDeclaredField("bmap"); raceBoostMap.setAccessible(true);
			ChoirDiagnostics.info("RACE compatibility class=" + RACE_CLASS + " expected=" + EXPECTED_RACE_SHA256 + " actual=" + hashes + " matched=" + compatible + " resources=" + hashes.size());
		} catch (Exception failure) {
			compatible = false; ChoirDiagnostics.error("RACE compatibility-check-failed cause=" + failure.getClass().getSimpleName() + ": " + failure.getMessage());
		}
		try {
			Enumeration<URL> resources = RacePreferrence.class.getClassLoader().getResources(PREFERENCE_CLASS);
			ArrayList<String> hashes = new ArrayList<String>();
			while (resources.hasMoreElements()) try (InputStream in = resources.nextElement().openStream()) { hashes.add(hex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()))); }
			preferenceCompatible = hashes.size() == 1 && EXPECTED_PREFERENCE_SHA256.equals(hashes.get(0));
			foodField = RacePreferrence.class.getField("food"); drinkField = RacePreferrence.class.getField("drink");
			foodMaskField = RacePreferrence.class.getField("foodMask"); drinkMaskField = RacePreferrence.class.getField("drinkMask");
			foodField.setAccessible(true); drinkField.setAccessible(true); foodMaskField.setAccessible(true); drinkMaskField.setAccessible(true);
			ChoirDiagnostics.info("RACE preference-compatibility class=" + PREFERENCE_CLASS + " expected="
					+ EXPECTED_PREFERENCE_SHA256 + " actual=" + hashes + " matched=" + preferenceCompatible + " resources=" + hashes.size());
		} catch (Exception failure) {
			preferenceCompatible = false;
			ChoirDiagnostics.error("RACE preference-compatibility-check-failed cause=" + failure.getClass().getSimpleName() + ": " + failure.getMessage());
		}
		RaceRegistry.preferenceAdapterReady(preferenceCompatible);
	}
	static synchronized void registryCycleStarted(long cycleId) {
		CyclePlan plan = RaceRegistry.cyclePlan(cycleId);
		if ((!plan.declarations().isEmpty() || !plan.patches().isEmpty()) && !compatible) throw new IllegalStateException("Race API is unavailable because the v71.44 Race fingerprint did not match.");
		LIST<Race> live = RACES.all();
		int registryIdentity = System.identityHashCode(live);
		if (cycleId == lastCycle && registryIdentity == lastRegistryIdentity) throw new IllegalStateException("Race platform materialization attempted twice in registry cycle " + cycleId);
		lastCycle = cycleId; lastRegistryIdentity = registryIdentity; applied.clear();
		Map<String, Race> races = new HashMap<String, Race>();
		for (Race race : live) if (races.put(race.key(), race) != null) throw new IllegalStateException("Duplicate live race ID: " + race.key());
		ArrayList<String> declarations = new ArrayList<String>();
		for (RaceDeclaration declaration : plan.declarations()) {
			if (!races.containsKey(declaration.raceId())) throw new IllegalStateException("Declared data-backed race is absent from RACES.all(): " + declaration.raceId());
			declarations.add(declaration.raceId());
			ChoirDiagnostics.info("RACE declaration-resolved cycle=" + cycleId + " provider=" + declaration.providerId() + " race=" + declaration.raceId());
		}
		ArrayList<String> targets = new ArrayList<String>();
		for (PatchPlan patch : plan.patches()) {
			Race race = races.get(patch.raceId());
			if (race == null) throw new IllegalStateException("Race patch target is absent from RACES.all(): " + patch.raceId());
			LIST<Boostable> boostables = BOOSTING.MAP().get(patch.boostableId());
			if (boostables == null || boostables.size() == 0) throw new IllegalStateException("Race patch boostable is absent from BOOSTING.MAP(): " + patch.boostableId());
			String materializationKey = registryIdentity + "\u0000" + patch.targetId();
			if (!applied.add(materializationKey)) throw new IllegalStateException("Duplicate race patch materialization: " + patch.targetId());
			BoosterImp booster = new BoosterImp(new BSourceInfo("Choir race patch", race.boosts.info.icon), patch.multiplier(), true);
			for (Boostable boostable : boostables) race.boosts.push(new BoostSpec(booster, boostable, null));
			try { raceBoostMap.set(race, null); } catch (IllegalAccessException failure) { throw new IllegalStateException("Could not invalidate Race.bmap after patch composition.", failure); }
			targets.add(patch.targetId());
			ArrayList<String> contributors = new ArrayList<String>();
			for (PatchContribution<Double> contribution : patch.contributions()) contributors.add(contribution.providerId() + "/" + contribution.patchId() + "=" + contribution.value());
			ChoirDiagnostics.info("RACE patch-materialized cycle=" + cycleId + " race=" + patch.raceId() + " boostable=" + patch.boostableId()
					+ " multiplier=" + patch.multiplier() + " contributors=" + contributors + " boostableMatches=" + boostables.size());
		}
		RaceRegistry.materialized(cycleId, declarations, targets);
		ChoirDiagnostics.info("RACE registry cycle=" + cycleId + " liveRaces=" + live.size() + " declarations=" + declarations.size()
				+ " patchTargets=" + targets.size() + " registryIdentity=" + registryIdentity + " state=CONFIRMED");
	}
	static synchronized void materializePreferences(long runtimeGeneration) {
		List<RaceRegistry.PreferenceTargetPlan> plans = RaceRegistry.preferencePlans();
		LIST<Race> live = RACES.all();
		int registryIdentity = System.identityHashCode(live);
		if (lastPreferenceRuntimeGeneration == runtimeGeneration && lastPreferenceRegistryIdentity == registryIdentity)
			throw new IllegalStateException("Race preference materialization attempted twice for runtime generation " + runtimeGeneration);
		lastPreferenceRuntimeGeneration = runtimeGeneration;
		lastPreferenceRegistryIdentity = registryIdentity;
		if (!plans.isEmpty() && !preferenceCompatible)
			throw new IllegalStateException("Race preference API is unavailable because the v71.44 RacePreferrence fingerprint did not match.");

		Map<String, Race> races = new HashMap<String, Race>();
		for (Race race : live) if (races.put(race.key(), race) != null) throw new IllegalStateException("Duplicate live race ID: " + race.key());
		Map<String, RESOURCE> resources = canonicalResources();
		ArrayList<String> materializedTargets = new ArrayList<String>();
		for (RaceRegistry.PreferenceTargetPlan plan : plans) {
			Race race = races.get(plan.raceId());
			if (race == null || race.pref() == null) throw new IllegalStateException("Race preference target is unavailable after RACES.expand(): " + plan.raceId());
			RacePreferrence preference = race.pref();
			LIST<ResG> current = plan.kind() == RacePreferenceKind.FOOD ? preference.food : preference.drink;
			ArrayList<String> base = new ArrayList<String>();
			for (ResG group : current) base.add(canonicalId(group.resource));
			RaceRegistry.PreferenceResolution resolution = RaceRegistry.resolvePreference(plan, base);
			snake2d.util.sets.ArrayList<ResG> replacement = new snake2d.util.sets.ArrayList<ResG>(resolution.resourceIds().size());
			RBITImp mask = new RBITImp();
			for (String resourceId : resolution.resourceIds()) {
				RESOURCE resource = resources.get(resourceId);
				if (resource == null) throw new IllegalStateException("Race preference resource is absent from RESOURCES.ALL(): " + resourceId);
				ResG group = plan.kind() == RacePreferenceKind.FOOD ? RESOURCES.EDI().get(resource) : RESOURCES.DRINKS().get(resource);
				if (group == null) throw new IllegalStateException("Race preference resource is not valid for " + plan.kind() + ": " + resourceId);
				replacement.add(group); mask.or(resource);
			}
			try {
				Field listField = plan.kind() == RacePreferenceKind.FOOD ? foodField : drinkField;
				Field maskField = plan.kind() == RacePreferenceKind.FOOD ? foodMaskField : drinkMaskField;
				listField.set(preference, replacement); maskField.set(preference, mask);
				if (listField.get(preference) != replacement || maskField.get(preference) != mask)
					throw new IllegalStateException("Reflected race preference assignment did not retain the verified replacement objects.");
			} catch (IllegalAccessException failure) {
				throw new IllegalStateException("Could not replace exact-v71.44 race preference data.", failure);
			}
			for (String resourceId : resolution.resourceIds()) if (!mask.has(resources.get(resourceId)))
				throw new IllegalStateException("Race preference mask verification failed for " + resourceId);
			materializedTargets.add(plan.targetId());
			ArrayList<String> contributors = new ArrayList<String>();
			for (RacePreferencePatch contribution : resolution.contributions()) contributors.add(contribution.providerId() + "/" + contribution.patchId()
					+ "=" + contribution.operation() + contribution.resourceIds() + "@" + contribution.priority());
			ChoirDiagnostics.info("RACE preference-materialized runtime.generation=" + runtimeGeneration + " target=" + plan.targetId()
					+ " base=" + base + " effective=" + resolution.resourceIds() + " contributors=" + contributors
					+ " diagnostics=" + resolution.diagnostics() + " preferenceIdentity=" + System.identityHashCode(preference));
		}
		RaceRegistry.preferencesMaterialized(runtimeGeneration, materializedTargets);
		ChoirDiagnostics.info("RACE preference-registry runtime.generation=" + runtimeGeneration + " liveRaces=" + live.size()
				+ " targets=" + materializedTargets.size() + " registryIdentity=" + registryIdentity + " state=CONFIRMED");
	}
	private static Map<String, RESOURCE> canonicalResources() {
		HashMap<String, RESOURCE> result = new HashMap<String, RESOURCE>();
		for (RESOURCE resource : RESOURCES.ALL()) {
			String id = canonicalId(resource);
			if (result.put(id, resource) != null) throw new IllegalStateException("Duplicate canonical resource ID: " + id);
		}
		return result;
	}
	private static String canonicalId(RESOURCE resource) {
		String internal = resource.key();
		String candidate = V7144ResourceStableIds.publicAliasCandidate(internal);
		RESOURCE alias = RESOURCES.map().tryGet(candidate);
		return V7144ResourceStableIds.canonical(internal, alias == resource);
	}
	static synchronized void disposed() {
		lastCycle = 0; lastRegistryIdentity = 0; applied.clear();
		lastPreferenceRuntimeGeneration = 0; lastPreferenceRegistryIdentity = 0;
	}
	private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(); for (byte value : bytes) out.append(String.format("%02x", value & 255)); return out.toString(); }
}
