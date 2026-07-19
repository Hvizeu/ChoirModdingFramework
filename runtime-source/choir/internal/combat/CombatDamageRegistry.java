package choir.internal.combat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import choir.api.combat.CombatDamageCategory;
import choir.api.combat.CombatDamageModifier;
import choir.api.combat.CombatDamageRegistrationResult;
import choir.api.combat.CombatDamageRuntimeSnapshot;
import choir.api.combat.CombatExecutionMode;
import choir.api.combat.CombatParticipantSide;
import choir.internal.ChoirDiagnostics;

/** Process-retained descriptors and an immutable, allocation-free tactical evaluation plan. */
public final class CombatDamageRegistry {
	private static final Map<String, CombatDamageModifier> modifiers = new TreeMap<String, CombatDamageModifier>();
	private static final Set<String> firstApplicationDiagnostics = ConcurrentHashMap.newKeySet();
	private static volatile Plan plan = Plan.empty();
	private static long generation;
	private static final AtomicLong applications = new AtomicLong();
	private static final AtomicLong failures = new AtomicLong();
	private static volatile boolean adapterReady;
	private static String adapterDetail = "not initialized";

	private CombatDamageRegistry() { }

	public static synchronized CombatDamageRegistrationResult register(CombatDamageModifier modifier) {
		if (modifier == null) throw new IllegalArgumentException("Combat damage modifier is required.");
		String identity = modifier.providerId() + '\u0000' + modifier.modifierId();
		CombatDamageModifier old = modifiers.get(identity);
		CombatDamageRegistrationResult result;
		if (old == null) {
			modifiers.put(identity, modifier);
			generation++;
			rebuild();
			result = CombatDamageRegistrationResult.ACCEPTED;
		} else if (equivalent(old, modifier)) result = CombatDamageRegistrationResult.IDEMPOTENT;
		else result = CombatDamageRegistrationResult.REJECTED_CONFLICT;
		ChoirDiagnostics.info("COMBAT-DAMAGE registration provider=" + modifier.providerId()
				+ " modifier=" + modifier.modifierId() + " categories=" + modifier.categories()
				+ " attacker=" + modifier.attackerSide() + " defender=" + modifier.defenderSide()
				+ " priority=" + modifier.priority() + " multiplier=" + modifier.multiplier()
				+ " result=" + result + " generation=" + generation + " plan=" + plan.signature);
		return result;
	}

	public static synchronized void adapterReady(boolean ready, String detail) {
		adapterReady = ready;
		adapterDetail = detail == null ? "" : detail;
		ChoirDiagnostics.info("COMBAT-DAMAGE adapter-ready=" + ready + " detail=" + adapterDetail
				+ " registrations=" + modifiers.size() + " plan=" + plan.signature);
	}

	public static boolean capabilityReady() { return adapterReady; }

	/** Applies a compiled plan. Null attacker ownership means only ANY predicates can match. */
	public static double apply(CombatDamageCategory category, Boolean attackerPlayer,
			boolean defenderPlayer, double vanillaDamage) {
		if (!adapterReady || category == null || !Double.isFinite(vanillaDamage) || vanillaDamage <= 0.0)
			return vanillaDamage;
		Plan current = plan;
		int attackerIndex = attackerPlayer == null ? 2 : (attackerPlayer.booleanValue() ? 0 : 1);
		int defenderIndex = defenderPlayer ? 0 : 1;
		double factor = current.factors[category.ordinal()][attackerIndex][defenderIndex];
		int matched = current.counts[category.ordinal()][attackerIndex][defenderIndex];
		if (matched == 0) return vanillaDamage;
		double effective = vanillaDamage * factor;
		if (!Double.isFinite(factor) || !Double.isFinite(effective) || effective < 0.0) {
			failures.incrementAndGet();
			ChoirDiagnostics.error("COMBAT-DAMAGE composition-failure category=" + category
					+ " attackerPlayer=" + attackerPlayer + " defenderPlayer=" + defenderPlayer
					+ " vanilla=" + vanillaDamage + " factor=" + factor + " plan=" + current.signature
					+ " action=preserve-vanilla");
			return vanillaDamage;
		}
		applications.incrementAndGet();
		String diagnosticKey = current.signature + ':' + category + ':' + attackerPlayer + ':' + defenderPlayer;
		if (firstApplicationDiagnostics.add(diagnosticKey))
			ChoirDiagnostics.info("COMBAT-DAMAGE first-application category=" + category
					+ " attackerPlayer=" + attackerPlayer + " defenderPlayer=" + defenderPlayer
					+ " contributions=" + matched + " factor=" + factor + " plan=" + current.signature);
		return effective;
	}

	public static synchronized CombatDamageRuntimeSnapshot snapshot() {
		return new CombatDamageRuntimeSnapshot(generation, modifiers.size(), adapterReady,
				plan.signature, applications.get(), failures.get());
	}

	/** Live combat handles are adapter-owned; process descriptors deliberately survive disposal. */
	public static synchronized void disposeRuntime() {
		firstApplicationDiagnostics.clear();
		ChoirDiagnostics.info("COMBAT-DAMAGE runtime-disposed registrations-retained=" + modifiers.size()
				+ " applications=" + applications.get() + " failures=" + failures.get() + " plan=" + plan.signature);
	}

	static synchronized void resetForTests() {
		modifiers.clear(); firstApplicationDiagnostics.clear(); generation = 0; applications.set(0); failures.set(0);
		adapterReady = false; adapterDetail = "test reset"; plan = Plan.empty();
	}

	private static void rebuild() {
		ArrayList<CombatDamageModifier> ordered = new ArrayList<CombatDamageModifier>(modifiers.values());
		ordered.sort(Comparator.comparingInt(CombatDamageModifier::priority)
				.thenComparing(CombatDamageModifier::providerId).thenComparing(CombatDamageModifier::modifierId));
		double[][][] factors = new double[CombatDamageCategory.values().length][3][2];
		int[][][] counts = new int[CombatDamageCategory.values().length][3][2];
		for (CombatDamageCategory category : CombatDamageCategory.values()) {
			for (int attacker = 0; attacker < 3; attacker++) for (int defender = 0; defender < 2; defender++) {
				Boolean attackerPlayer = attacker == 2 ? null : Boolean.valueOf(attacker == 0);
				Boolean defenderPlayer = Boolean.valueOf(defender == 0);
				double factor = 1.0; int count = 0;
				for (CombatDamageModifier modifier : ordered) {
					if (modifier.executionMode() != CombatExecutionMode.TACTICAL_SETTLEMENT
							|| !modifier.categories().contains(category)
							|| !matches(modifier.attackerSide(), attackerPlayer)
							|| !matches(modifier.defenderSide(), defenderPlayer)) continue;
					factor *= modifier.multiplier(); count++;
				}
				factors[category.ordinal()][attacker][defender] = factor;
				counts[category.ordinal()][attacker][defender] = count;
			}
		}
		plan = new Plan(factors, counts, signature(ordered));
		firstApplicationDiagnostics.clear();
	}

	private static boolean matches(CombatParticipantSide predicate, Boolean player) {
		if (predicate == CombatParticipantSide.ANY) return true;
		if (player == null) return false;
		return predicate == CombatParticipantSide.PLAYER ? player.booleanValue() : !player.booleanValue();
	}

	private static boolean equivalent(CombatDamageModifier a, CombatDamageModifier b) {
		return a.providerId().equals(b.providerId()) && a.modifierId().equals(b.modifierId())
				&& a.categories().equals(b.categories()) && a.attackerSide() == b.attackerSide()
				&& a.defenderSide() == b.defenderSide() && a.executionMode() == b.executionMode()
				&& a.priority() == b.priority() && Double.compare(a.multiplier(), b.multiplier()) == 0;
	}

	private static String signature(List<CombatDamageModifier> ordered) {
		StringBuilder input = new StringBuilder();
		for (CombatDamageModifier modifier : ordered) input.append(modifier.priority()).append('|')
				.append(modifier.providerId()).append('|').append(modifier.modifierId()).append('|')
				.append(modifier.executionMode()).append('|').append(modifier.attackerSide()).append('|')
				.append(modifier.defenderSide()).append('|').append(modifier.categories()).append('|')
				.append(Double.toHexString(modifier.multiplier())).append('\n');
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.toString().getBytes(StandardCharsets.UTF_8));
			StringBuilder value = new StringBuilder();
			for (byte item : hash) value.append(String.format("%02x", item & 255));
			return value.toString();
		} catch (Exception impossible) { throw new IllegalStateException(impossible); }
	}

	private static final class Plan {
		final double[][][] factors;
		final int[][][] counts;
		final String signature;
		Plan(double[][][] factors, int[][][] counts, String signature) {
			this.factors = factors; this.counts = counts; this.signature = signature;
		}
		static Plan empty() {
			double[][][] factors = new double[CombatDamageCategory.values().length][3][2];
			for (int c=0;c<factors.length;c++) for(int a=0;a<3;a++) for(int d=0;d<2;d++) factors[c][a][d]=1.0;
			return new Plan(factors, new int[CombatDamageCategory.values().length][3][2], signature(List.of()));
		}
	}
}
