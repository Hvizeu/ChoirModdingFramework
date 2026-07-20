package choir.internal.storage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import choir.api.storage.ChoirStorage;
import choir.api.storage.MultiResourceStorageDeclaration;
import choir.api.storage.MultiResourceStorageRegistrationResult;
import choir.api.storage.MultiResourceStorageRuntimeSnapshot;
import choir.internal.ChoirDiagnostics;
import choir.internal.platform.PlatformRuntime;
import choir.internal.production.MultiOutputProductionRegistry;

/** Process-retained room policies and lightweight runtime diagnostics. */
public final class MultiResourceStorageRegistry {
	private static final TreeMap<String, MultiResourceStorageDeclaration> declarations = new TreeMap<String, MultiResourceStorageDeclaration>();
	private static final TreeMap<String, String> targetOwners = new TreeMap<String, String>();
	private static volatile Plan plan = Plan.empty();
	private static long generation;
	private static volatile boolean adapterReady;
	private static String adapterDetail = "not initialized";
	private static final AtomicLong liveCells = new AtomicLong();
	private static final AtomicLong storedUnits = new AtomicLong();
	private static final AtomicLong pickupReservations = new AtomicLong();
	private static final AtomicLong incomingReservations = new AtomicLong();
	private static final AtomicLong failures = new AtomicLong();
	private static final String STOCKPILE_ROOM_KEY = "_STOCKPILE";

	private MultiResourceStorageRegistry() { }

	public static synchronized MultiResourceStorageRegistrationResult register(MultiResourceStorageDeclaration declaration) {
		if (declaration == null) throw new IllegalArgumentException("Multi-resource storage declaration is required.");
		MultiResourceStorageDeclaration old = declarations.get(declaration.qualifiedId());
		MultiResourceStorageRegistrationResult result;
		if (old != null) result = old.equals(declaration)
				? MultiResourceStorageRegistrationResult.IDEMPOTENT
				: MultiResourceStorageRegistrationResult.REJECTED_CONFLICT;
		else {
			String owner = targetOwners.get(declaration.roomKey());
			if (owner != null) result = MultiResourceStorageRegistrationResult.REJECTED_TARGET_CONFLICT;
			else {
				declarations.put(declaration.qualifiedId(), declaration);
				targetOwners.put(declaration.roomKey(), declaration.qualifiedId());
				generation++;
				rebuild();
				result = MultiResourceStorageRegistrationResult.ACCEPTED;
			}
		}
		ChoirDiagnostics.info("MULTI-STORAGE registration provider=" + declaration.providerId()
				+ " declaration=" + declaration.declarationId() + " room=" + declaration.roomKey()
				+ " maxKinds=" + declaration.maxResourceKinds() + " result=" + result
				+ " generation=" + generation + " plan=" + plan.signature);
		return result;
	}

	public static synchronized void adapterReady(boolean ready, String detail) {
		adapterReady = ready;
		adapterDetail = detail == null ? "" : detail;
		ChoirDiagnostics.info("MULTI-STORAGE adapter-ready=" + ready + " defaultMaxKinds="
				+ ChoirStorage.DEFAULT_MAX_RESOURCE_KINDS + " hardMaxKinds="
				+ ChoirStorage.HARD_MAX_RESOURCE_KINDS + " detail=" + adapterDetail);
	}

	public static boolean capabilityReady() { return adapterReady; }
	public static long generation() { return generation; }

	/** Returns whether an active provider explicitly opted this room into advanced storage. */
	public static boolean hasActiveRoomPolicy(String roomKey) {
		MultiResourceStorageDeclaration declaration = declaration(roomKey);
		return declaration != null && PlatformRuntime.isActive(declaration.providerId());
	}

	/**
	 * Resolves room-wide activation. Multi-output and explicit room declarations
	 * remain advanced even when the vanilla-room extension is disabled.
	 */
	public static boolean requiresAdvancedStorage(String roomKey) {
		if (MultiOutputProductionRegistry.hasActiveTarget(roomKey)) return true;
		if (hasActiveRoomPolicy(roomKey)) return true;
		return supportsGlobalAdvancedStorage(roomKey) && AdvancedStoragePolicy.advancedVanillaRoomsEnabled();
	}

	/** Resolves activation for one exact production industry. */
	public static boolean requiresAdvancedStorage(String roomKey, int industryIndex) {
		if (MultiOutputProductionRegistry.hasActiveTarget(roomKey, industryIndex)) return true;
		if (hasActiveRoomPolicy(roomKey)) return true;
		return AdvancedStoragePolicy.advancedVanillaRoomsEnabled();
	}

	public static int maxResourceKinds(String roomKey) {
		MultiResourceStorageDeclaration declaration = activeDeclaration(roomKey);
		if (declaration != null) return declaration.maxResourceKinds();
		return requiresAdvancedStorage(roomKey) ? ChoirStorage.DEFAULT_MAX_RESOURCE_KINDS
				: ChoirStorage.VANILLA_MAX_RESOURCE_KINDS;
	}

	public static int maxResourceKinds(String roomKey, int industryIndex) {
		MultiResourceStorageDeclaration declaration = activeDeclaration(roomKey);
		if (declaration != null) return declaration.maxResourceKinds();
		return requiresAdvancedStorage(roomKey, industryIndex) ? ChoirStorage.DEFAULT_MAX_RESOURCE_KINDS
				: ChoirStorage.VANILLA_MAX_RESOURCE_KINDS;
	}

	/** Limit for a room already latched/sticky as advanced, independent of the global option. */
	public static int advancedMaxResourceKinds(String roomKey) {
		MultiResourceStorageDeclaration declaration = activeDeclaration(roomKey);
		return declaration == null ? ChoirStorage.DEFAULT_MAX_RESOURCE_KINDS : declaration.maxResourceKinds();
	}

	public static void cellCreated(int stored, int pickupReserved, int incomingReserved) {
		liveCells.incrementAndGet();
		storedUnits.addAndGet(stored);
		pickupReservations.addAndGet(pickupReserved);
		incomingReservations.addAndGet(incomingReserved);
	}

	public static void cellDisposed(int stored, int pickupReserved, int incomingReserved) {
		liveCells.decrementAndGet();
		storedUnits.addAndGet(-stored);
		pickupReservations.addAndGet(-pickupReserved);
		incomingReservations.addAndGet(-incomingReserved);
	}

	public static void recordDelta(int storedDelta, int pickupReservationDelta, int incomingReservationDelta) {
		storedUnits.addAndGet(storedDelta);
		pickupReservations.addAndGet(pickupReservationDelta);
		incomingReservations.addAndGet(incomingReservationDelta);
	}

	public static void recordFailure(String detail) {
		failures.incrementAndGet();
		ChoirDiagnostics.error("MULTI-STORAGE failure detail=" + detail);
	}

	public static synchronized MultiResourceStorageRuntimeSnapshot snapshot() {
		return new MultiResourceStorageRuntimeSnapshot(generation, declarations.size(), adapterReady,
				plan.signature, ChoirStorage.DEFAULT_MAX_RESOURCE_KINDS, ChoirStorage.HARD_MAX_RESOURCE_KINDS,
				liveCells.get(), storedUnits.get(), pickupReservations.get(), incomingReservations.get(), failures.get());
	}

	public static void disposeRuntime() {
		liveCells.set(0); storedUnits.set(0); pickupReservations.set(0); incomingReservations.set(0);
		AdvancedStoragePolicy.clearWorldLatch();
		ChoirDiagnostics.info("MULTI-STORAGE runtime-disposed registrations-retained=" + declarations.size());
	}

	static synchronized void resetForTests() {
		declarations.clear(); targetOwners.clear(); plan = Plan.empty(); generation = 0;
		adapterReady = false; adapterDetail = "test reset"; liveCells.set(0); storedUnits.set(0);
		pickupReservations.set(0); incomingReservations.set(0); failures.set(0);
		AdvancedStoragePolicy.resetForTests();
	}

	private static MultiResourceStorageDeclaration declaration(String roomKey) {
		return roomKey == null ? null : plan.byRoom.get(roomKey);
	}

	private static MultiResourceStorageDeclaration activeDeclaration(String roomKey) {
		MultiResourceStorageDeclaration declaration = declaration(roomKey);
		return declaration != null && PlatformRuntime.isActive(declaration.providerId()) ? declaration : null;
	}

	private static boolean supportsGlobalAdvancedStorage(String roomKey) {
		return STOCKPILE_ROOM_KEY.equals(roomKey);
	}

	private static void rebuild() {
		TreeMap<String, MultiResourceStorageDeclaration> byRoom = new TreeMap<String, MultiResourceStorageDeclaration>();
		ArrayList<MultiResourceStorageDeclaration> ordered = new ArrayList<MultiResourceStorageDeclaration>(declarations.values());
		for (MultiResourceStorageDeclaration declaration : ordered) byRoom.put(declaration.roomKey(), declaration);
		plan = new Plan(Map.copyOf(byRoom), signature(ordered));
	}

	private static String signature(List<MultiResourceStorageDeclaration> ordered) {
		StringBuilder input = new StringBuilder();
		for (MultiResourceStorageDeclaration declaration : ordered)
			input.append(declaration.qualifiedId()).append('|').append(declaration.roomKey()).append('|')
					.append(declaration.maxResourceKinds()).append('\n');
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.toString().getBytes(StandardCharsets.UTF_8));
			StringBuilder value = new StringBuilder();
			for (byte item : hash) value.append(String.format("%02x", item & 255));
			return value.toString();
		} catch (Exception impossible) { throw new IllegalStateException(impossible); }
	}

	private static final class Plan {
		final Map<String, MultiResourceStorageDeclaration> byRoom;
		final String signature;
		Plan(Map<String, MultiResourceStorageDeclaration> byRoom, String signature) {
			this.byRoom = byRoom; this.signature = signature;
		}
		static Plan empty() { return new Plan(Map.of(), signature(List.of())); }
	}
}
