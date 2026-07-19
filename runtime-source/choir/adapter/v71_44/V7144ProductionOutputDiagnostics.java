package choir.adapter.v71_44;

import choir.internal.diagnostics.DecisionDiagnostics;
import init.resources.RESOURCE;
import settlement.room.industry.module.ROOM_PRODUCER_INSTANCE;
import settlement.room.main.RoomInstance;

/** Bounded diagnostics for primary-output availability and room-storage handoff. */
public final class V7144ProductionOutputDiagnostics {
	private V7144ProductionOutputDiagnostics() { }

	public static void coordinatesReconciled(String family, RoomInstance room,
			int changedCells, int totalCells) {
		if (room == null || changedCells <= 0) return;
		String roomKey = room.blueprintI().key;
		int industry = room instanceof ROOM_PRODUCER_INSTANCE
				? ((ROOM_PRODUCER_INSTANCE) room).industryI() : -1;
		int attempts = DecisionDiagnostics.permitProblem("production-storage-coordinates/"
				+ family + '/' + roomKey + '/' + industry + '/' + room.mX() + ',' + room.mY());
		DecisionDiagnostics.problem("PRODUCTION_STORAGE_COORDINATES_RECONCILED", attempts,
				"family=" + family + " room=" + roomKey + " industry=" + industry
				+ " room.master=" + room.mX() + ',' + room.mY()
				+ " changed.cells=" + changedCells + " total.cells=" + totalCells);
	}

	public static void sidecarMigrated(String family, RoomInstance room,
			int savedSchema, int currentSchema, int savedCells, int actualCells) {
		if (room == null) return;
		String roomKey = room.blueprintI().key;
		int industry = room instanceof ROOM_PRODUCER_INSTANCE
				? ((ROOM_PRODUCER_INSTANCE) room).industryI() : -1;
		int attempts = DecisionDiagnostics.permitDetail("production-storage-save-migration/"
				+ family + '/' + roomKey + '/' + industry + '/' + room.index());
		DecisionDiagnostics.detail("PRODUCTION_STORAGE_SAVE_MIGRATED", attempts,
				"family=" + family + " room=" + roomKey + " industry=" + industry
				+ " schema.saved=" + savedSchema + " schema.current=" + currentSchema
				+ " cells.saved=" + savedCells + " cells.actual=" + actualCells);
	}

	public static void availabilityReconciled(String family, RoomInstance room,
			int cachedX, int cachedY, V7144ProductionStorageState state) {
		if (room == null || state == null) return;
		String roomKey = room.blueprintI().key;
		int industry = room instanceof ROOM_PRODUCER_INSTANCE
				? ((ROOM_PRODUCER_INSTANCE) room).industryI() : -1;
		int attempts = DecisionDiagnostics.permitProblem("production-storage-availability/"
				+ family + '/' + roomKey + '/' + industry + '/' + cachedX + ',' + cachedY);
		DecisionDiagnostics.problem("PRODUCTION_STORAGE_AVAILABILITY_RECONCILED", attempts,
				"family=" + family + " room=" + roomKey + " industry=" + industry
				+ " cached.start=" + cachedX + ',' + cachedY
				+ " cached.start.is.cell=" + (state.cell(cachedX, cachedY) != null)
				+ " actual.cells=" + state.size() + " shared.room=true");
	}

	public static void storeResult(String family, RoomInstance room, RESOURCE resource,
			int requested, int completed, int cachedX, int cachedY,
			V7144ProductionStorageState state) {
		if (room == null || resource == null || requested <= 0) return;
		String roomKey = room.blueprintI().key;
		int industry = room instanceof ROOM_PRODUCER_INSTANCE
				? ((ROOM_PRODUCER_INSTANCE) room).industryI() : -1;
		String resourceId = V7144MultiResourceStorage.stableResourceId(resource);
		String stableKey = "production-output/" + family + '/' + roomKey + '/' + industry
				+ '/' + resourceId + '/' + cachedX + ',' + cachedY;
		String detail = "family=" + family + " room=" + roomKey + " industry=" + industry
				+ " resource=" + resourceId + " requested=" + requested + " completed=" + completed
				+ " remaining=" + Math.max(0, requested-completed)
				+ " cached.start=" + cachedX + ',' + cachedY
				+ " cached.start.is.cell=" + (state != null && state.cell(cachedX, cachedY) != null)
				+ " actual.cells=" + (state == null ? 0 : state.size())
				+ " shared.room.after=" + (state != null && state.hasSharedRoom());
		if (completed < requested) {
			int attempts = DecisionDiagnostics.permitProblem(stableKey);
			DecisionDiagnostics.problem("PRODUCTION_OUTPUT_STALLED", attempts, detail);
		} else {
			int attempts = DecisionDiagnostics.permitDetail(stableKey);
			DecisionDiagnostics.detail("PRODUCTION_OUTPUT_STORED", attempts, detail);
		}
	}
}
