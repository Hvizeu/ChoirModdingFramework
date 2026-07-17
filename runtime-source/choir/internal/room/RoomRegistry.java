package choir.internal.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import choir.api.room.RoomDeclaration;
import choir.api.room.RoomRegistrationResult;
import choir.api.room.RoomRegistrationSnapshot;
import choir.api.room.RoomRegistrationStatus;
import choir.api.room.RoomRegistrationView;
import choir.internal.ChoirDiagnostics;
import choir.internal.platform.PlatformRuntime;

/** Process-retained declarations with generation-bound materialization state. */
public final class RoomRegistry {
	private static final TreeMap<String, Record> records = new TreeMap<String, Record>();
	private static final TreeMap<String, String> roomKeyOwners = new TreeMap<String, String>();
	private static long registrationGeneration;
	private static long currentCycleId;
	private static int materializedThisCycle;
	private static int totalMaterializations;
	private static boolean cycleComplete;
	private static boolean adapterCompatible;
	private static String adapterDetail = "adapter not initialized";
	private static List<RoomDeclaration> currentSnapshot = List.of();

	private RoomRegistry() { }

	public static synchronized RoomRegistrationResult register(RoomDeclaration declaration) {
		if (declaration == null) throw new IllegalArgumentException("Room declaration must not be null.");
		String identity = declaration.qualifiedId();
		Record old = records.get(identity);
		if (old != null) {
			if (old.declaration.equals(declaration)) {
				ChoirDiagnostics.info("ROOM-REGISTRATION idempotent provider=" + declaration.providerId() + " key=" + declaration.roomKey());
				return result(RoomRegistrationStatus.IDEMPOTENT, declaration, "identical declaration already retained");
			}
			return conflict(declaration, "declaration identity already retained with different content");
		}
		if (currentCycleId != 0)
			return late(declaration, "new room declarations are closed after the first registry snapshot");
		String owner = roomKeyOwners.get(declaration.roomKey());
		if (owner != null) return conflict(declaration, "room key already owned by " + owner);
		records.put(identity, new Record(declaration));
		roomKeyOwners.put(declaration.roomKey(), declaration.providerId());
		registrationGeneration++;
		ChoirDiagnostics.info("ROOM-REGISTRATION accepted provider=" + declaration.providerId() + " key=" + declaration.roomKey()
				+ " family=" + declaration.family() + " missingProviderPolicy=" + declaration.missingProviderPolicy()
				+ " generation=" + registrationGeneration);
		return result(RoomRegistrationStatus.ACCEPTED, declaration, "retained for each compatible room registry");
	}

	public static synchronized void adapterReady(boolean compatible, String detail) {
		adapterCompatible = compatible;
		adapterDetail = detail == null ? "<none>" : detail;
		ChoirDiagnostics.info("ROOM-REGISTRATION adapter-compatible=" + compatible + " detail=" + adapterDetail);
	}

	public static synchronized boolean capabilityAvailable() { return adapterCompatible; }
	public static synchronized int pendingCount() { return records.size(); }
	public static synchronized int totalMaterializations() { return totalMaterializations; }

	public static synchronized List<RoomDeclaration> snapshotForCycle(long cycleId) {
		if (cycleId <= 0) throw new IllegalArgumentException("Registry cycle ID must be positive.");
		if (currentCycleId == cycleId) return currentSnapshot;
		if (currentCycleId != 0 && !cycleComplete)
			throw new IllegalStateException("Nested stable room registry cycle: current=" + currentCycleId + " next=" + cycleId);
		if (!records.isEmpty() && !adapterCompatible)
			throw new IllegalStateException("Stable room adapter is incompatible: " + adapterDetail);
		ArrayList<RoomDeclaration> declarations = new ArrayList<RoomDeclaration>();
		for (Record record : records.values()) {
			if (!PlatformRuntime.isActive(record.declaration.providerId()))
				throw new IllegalStateException("Room provider manifest is not active: " + record.declaration.providerId());
			record.currentCycleId = 0;
			record.currentRuntimeIndex = -1;
			declarations.add(record.declaration);
		}
		currentCycleId = cycleId;
		materializedThisCycle = 0;
		cycleComplete = false;
		currentSnapshot = Collections.unmodifiableList(declarations);
		ChoirDiagnostics.info("ROOM-REGISTRATION snapshot cycle=" + cycleId + " declarations=" + currentSnapshot.size()
				+ " registrationGeneration=" + registrationGeneration);
		return currentSnapshot;
	}

	public static synchronized void materialized(long cycleId, RoomDeclaration declaration, int runtimeIndex) {
		if (cycleId != currentCycleId || cycleComplete)
			throw new IllegalStateException("Room materialization outside active cycle: " + cycleId);
		Record record = records.get(declaration.qualifiedId());
		if (record == null || !record.declaration.equals(declaration))
			throw new IllegalStateException("Room declaration was not retained: " + declaration.qualifiedId());
		if (record.currentCycleId == cycleId)
			throw new IllegalStateException("Duplicate room materialization in cycle=" + cycleId + " key=" + declaration.roomKey());
		if (runtimeIndex < 0) throw new IllegalArgumentException("Runtime room index must be non-negative.");
		record.currentCycleId = cycleId;
		record.currentRuntimeIndex = runtimeIndex;
		record.totalMaterializations++;
		materializedThisCycle++;
		totalMaterializations++;
		ChoirDiagnostics.info("ROOM-REGISTRATION materialized cycle=" + cycleId + " provider=" + declaration.providerId()
				+ " key=" + declaration.roomKey() + " runtimeIndex=" + runtimeIndex);
	}

	public static synchronized void completeCycle(long cycleId) {
		if (cycleId != currentCycleId || cycleComplete)
			throw new IllegalStateException("Stable room cycle completion mismatch: " + cycleId);
		if (materializedThisCycle != currentSnapshot.size())
			throw new IllegalStateException("Stable room materialization count mismatch: expected=" + currentSnapshot.size()
					+ " actual=" + materializedThisCycle);
		cycleComplete = true;
		ChoirDiagnostics.info("ROOM-REGISTRATION complete cycle=" + cycleId + " materialized=" + materializedThisCycle);
	}

	public static synchronized void failCycle(long cycleId) {
		if (currentCycleId == cycleId) cycleComplete = false;
	}

	public static synchronized void registryDisposed() {
		currentCycleId = 0;
		materializedThisCycle = 0;
		cycleComplete = true;
		currentSnapshot = List.of();
		for (Record record : records.values()) {
			record.currentCycleId = 0;
			record.currentRuntimeIndex = -1;
		}
		ChoirDiagnostics.info("ROOM-REGISTRATION live registry disposed retainedDeclarations=" + records.size());
	}

	public static synchronized RoomRegistrationSnapshot snapshot() {
		ArrayList<RoomRegistrationView> views = new ArrayList<RoomRegistrationView>();
		for (Record record : records.values()) views.add(new RoomRegistrationView(record.declaration,
				record.currentCycleId, record.currentRuntimeIndex, record.totalMaterializations));
		return new RoomRegistrationSnapshot(registrationGeneration, currentCycleId, materializedThisCycle,
				totalMaterializations, views);
	}

	private static RoomRegistrationResult conflict(RoomDeclaration declaration, String detail) {
		ChoirDiagnostics.error("ROOM-REGISTRATION conflict provider=" + declaration.providerId() + " key="
				+ declaration.roomKey() + " detail=" + detail);
		return result(RoomRegistrationStatus.CONFLICT, declaration, detail);
	}

	private static RoomRegistrationResult late(RoomDeclaration declaration, String detail) {
		ChoirDiagnostics.error("ROOM-REGISTRATION late provider=" + declaration.providerId() + " key="
				+ declaration.roomKey() + " detail=" + detail);
		return result(RoomRegistrationStatus.REJECTED_LATE, declaration, detail);
	}

	private static RoomRegistrationResult result(RoomRegistrationStatus status, RoomDeclaration declaration, String detail) {
		return new RoomRegistrationResult(status, declaration.providerId(), declaration.roomKey(), detail);
	}

	private static final class Record {
		final RoomDeclaration declaration;
		long currentCycleId;
		int currentRuntimeIndex = -1;
		int totalMaterializations;
		Record(RoomDeclaration declaration) { this.declaration = declaration; }
	}
}
