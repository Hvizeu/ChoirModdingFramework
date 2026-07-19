package choir.internal.production;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import choir.api.production.MultiOutputProductionRuntimeSnapshot;
import choir.api.production.MultiOutputRegistrationResult;
import choir.api.production.MultiOutputRoomDeclaration;
import choir.api.production.ProductionRoomFamily;
import choir.internal.ChoirDiagnostics;
import choir.internal.platform.PlatformRuntime;

/** Process-retained declarations and generation-safe production diagnostics. */
public final class MultiOutputProductionRegistry {
	private static final TreeMap<String, MultiOutputRoomDeclaration> declarations = new TreeMap<String, MultiOutputRoomDeclaration>();
	private static final TreeMap<String, String> targetOwners = new TreeMap<String, String>();
	private static final Set<String> validationDiagnostics = new HashSet<String>();
	private static volatile Plan plan = Plan.empty();
	private static long generation;
	private static volatile boolean adapterReady;
	private static String adapterDetail = "not initialized";
	private static final AtomicLong completedCycles = new AtomicLong();
	private static final AtomicLong emittedStacks = new AtomicLong();
	private static final AtomicLong emittedUnits = new AtomicLong();
	private static final AtomicLong failures = new AtomicLong();

	private MultiOutputProductionRegistry() { }

	public static synchronized MultiOutputRegistrationResult register(MultiOutputRoomDeclaration declaration) {
		if (declaration == null) throw new IllegalArgumentException("Multi-output room declaration is required.");
		MultiOutputRoomDeclaration old = declarations.get(declaration.qualifiedId());
		MultiOutputRegistrationResult result;
		if (old != null) result = old.equals(declaration)
				? MultiOutputRegistrationResult.IDEMPOTENT : MultiOutputRegistrationResult.REJECTED_CONFLICT;
		else {
			String owner = targetOwners.get(declaration.targetId());
			if (owner != null) result = MultiOutputRegistrationResult.REJECTED_TARGET_CONFLICT;
			else {
				declarations.put(declaration.qualifiedId(), declaration);
				targetOwners.put(declaration.targetId(), declaration.qualifiedId());
				generation++;
				rebuild();
				result = MultiOutputRegistrationResult.ACCEPTED;
			}
		}
		ChoirDiagnostics.info("MULTI-OUTPUT registration provider=" + declaration.providerId()
				+ " declaration=" + declaration.declarationId() + " target=" + declaration.targetId()
				+ " family=" + declaration.family() + " input=" + declaration.inputResourceId()
				+ " outputs=" + declaration.outputResourceIds() + " result=" + result
				+ " generation=" + generation + " plan=" + plan.signature);
		return result;
	}

	public static synchronized void adapterReady(boolean ready, String detail) {
		adapterReady = ready;
		adapterDetail = detail == null ? "" : detail;
		ChoirDiagnostics.info("MULTI-OUTPUT adapter-ready=" + ready + " detail=" + adapterDetail
				+ " registrations=" + declarations.size() + " plan=" + plan.signature);
	}

	public static boolean capabilityReady() { return adapterReady; }
	public static long generation() { return generation; }

	/** Returns whether an active provider declared any multi-output industry for this room. */
	public static boolean hasActiveTarget(String roomKey) {
		if (roomKey == null) return false;
		List<MultiOutputRoomDeclaration> room = plan.byRoom.get(roomKey);
		if (room == null) return false;
		for (MultiOutputRoomDeclaration declaration : room)
			if (PlatformRuntime.isActive(declaration.providerId())) return true;
		return false;
	}

	/** Returns whether an active provider declared this exact room/industry target. */
	public static boolean hasActiveTarget(String roomKey, int industryIndex) {
		if (roomKey == null || industryIndex < 0) return false;
		MultiOutputRoomDeclaration declaration = plan.byTarget.get(roomKey + "#" + industryIndex);
		return declaration != null && PlatformRuntime.isActive(declaration.providerId());
	}

	/** Resolves and validates one live recipe signature without retaining game objects. */
	public static synchronized MultiOutputRoomDeclaration resolve(String roomKey, int industryIndex,
			ProductionRoomFamily family, List<String> inputResourceIds, List<String> outputResourceIds) {
		if (!adapterReady) return null;
		String target = roomKey + "#" + industryIndex;
		MultiOutputRoomDeclaration declaration = plan.byTarget.get(target);
		if (declaration == null) return null;
		if (!PlatformRuntime.isActive(declaration.providerId()))
			return rejectOnce(declaration, "provider manifest is not active");
		if (declaration.family() != family)
			return rejectOnce(declaration, "family mismatch expected=" + declaration.family() + " actual=" + family);
		if (inputResourceIds.size() != 1 || !declaration.inputResourceId().equals(inputResourceIds.get(0)))
			return rejectOnce(declaration, "input mismatch expected=[" + declaration.inputResourceId() + "] actual=" + inputResourceIds);
		if (!declaration.outputResourceIds().equals(outputResourceIds))
			return rejectOnce(declaration, "output mismatch expected=" + declaration.outputResourceIds() + " actual=" + outputResourceIds);
		String key = declaration.qualifiedId() + "\u0000accepted";
		if (validationDiagnostics.add(key))
			ChoirDiagnostics.info("MULTI-OUTPUT live-recipe-validated declaration=" + declaration.qualifiedId()
					+ " target=" + declaration.targetId() + " outputs=" + outputResourceIds + " plan=" + plan.signature);
		return declaration;
	}

	public static void recordEmission(MultiOutputRoomDeclaration declaration, int stacks, int units) {
		completedCycles.incrementAndGet();
		emittedStacks.addAndGet(stacks);
		emittedUnits.addAndGet(units);
		if (completedCycles.get() == 1)
			ChoirDiagnostics.info("MULTI-OUTPUT first-emission declaration=" + declaration.qualifiedId()
					+ " target=" + declaration.targetId() + " stacks=" + stacks + " units=" + units
					+ " plan=" + plan.signature);
	}

	public static void recordFailure(String detail) {
		failures.incrementAndGet();
		ChoirDiagnostics.error("MULTI-OUTPUT execution-failure detail=" + detail + " action=preserve-primary-output");
	}

	public static synchronized MultiOutputProductionRuntimeSnapshot snapshot() {
		return new MultiOutputProductionRuntimeSnapshot(generation, declarations.size(), adapterReady,
				plan.signature, completedCycles.get(), emittedStacks.get(), emittedUnits.get(), failures.get());
	}

	public static synchronized void disposeRuntime() {
		validationDiagnostics.clear();
		ChoirDiagnostics.info("MULTI-OUTPUT runtime-disposed registrations-retained=" + declarations.size()
				+ " completedCycles=" + completedCycles.get() + " emittedUnits=" + emittedUnits.get());
	}

	static synchronized void resetForTests() {
		declarations.clear(); targetOwners.clear(); validationDiagnostics.clear(); generation = 0;
		adapterReady = false; adapterDetail = "test reset"; plan = Plan.empty();
		completedCycles.set(0); emittedStacks.set(0); emittedUnits.set(0); failures.set(0);
	}

	private static MultiOutputRoomDeclaration rejectOnce(MultiOutputRoomDeclaration declaration, String detail) {
		failures.incrementAndGet();
		String key = declaration.qualifiedId() + "\u0000" + detail;
		if (validationDiagnostics.add(key))
			ChoirDiagnostics.error("MULTI-OUTPUT live-recipe-rejected declaration=" + declaration.qualifiedId()
					+ " target=" + declaration.targetId() + " detail=" + detail);
		return null;
	}

	private static void rebuild() {
		TreeMap<String, MultiOutputRoomDeclaration> byTarget = new TreeMap<String, MultiOutputRoomDeclaration>();
		TreeMap<String, ArrayList<MultiOutputRoomDeclaration>> mutableByRoom =
				new TreeMap<String, ArrayList<MultiOutputRoomDeclaration>>();
		ArrayList<MultiOutputRoomDeclaration> ordered = new ArrayList<MultiOutputRoomDeclaration>(declarations.values());
		for (MultiOutputRoomDeclaration declaration : ordered) {
			byTarget.put(declaration.targetId(), declaration);
			mutableByRoom.computeIfAbsent(declaration.roomKey(), ignored ->
					new ArrayList<MultiOutputRoomDeclaration>()).add(declaration);
		}
		TreeMap<String, List<MultiOutputRoomDeclaration>> byRoom =
				new TreeMap<String, List<MultiOutputRoomDeclaration>>();
		for (Map.Entry<String, ArrayList<MultiOutputRoomDeclaration>> entry : mutableByRoom.entrySet())
			byRoom.put(entry.getKey(), List.copyOf(entry.getValue()));
		plan = new Plan(Map.copyOf(byTarget), Map.copyOf(byRoom), signature(ordered));
		validationDiagnostics.clear();
	}

	private static String signature(List<MultiOutputRoomDeclaration> ordered) {
		StringBuilder input = new StringBuilder();
		for (MultiOutputRoomDeclaration declaration : ordered) input.append(declaration.qualifiedId()).append('|')
				.append(declaration.targetId()).append('|').append(declaration.family()).append('|')
				.append(declaration.inputResourceId()).append('|').append(declaration.outputResourceIds()).append('\n');
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.toString().getBytes(StandardCharsets.UTF_8));
			StringBuilder value = new StringBuilder();
			for (byte item : hash) value.append(String.format("%02x", item & 255));
			return value.toString();
		} catch (Exception impossible) { throw new IllegalStateException(impossible); }
	}

	private static final class Plan {
		final Map<String, MultiOutputRoomDeclaration> byTarget;
		final Map<String, List<MultiOutputRoomDeclaration>> byRoom;
		final String signature;
		Plan(Map<String, MultiOutputRoomDeclaration> byTarget,
				Map<String, List<MultiOutputRoomDeclaration>> byRoom, String signature) {
			this.byTarget = byTarget; this.byRoom = byRoom; this.signature = signature;
		}
		static Plan empty() { return new Plan(Map.of(), Map.of(), signature(List.of())); }
	}
}
