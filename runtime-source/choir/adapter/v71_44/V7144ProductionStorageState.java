package choir.adapter.v71_44;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import choir.internal.storage.MultiResourceCell;
import choir.internal.storage.MultiResourceStorageRegistry;
import settlement.main.SETT;
import settlement.room.main.RoomInstance;
import snake2d.util.datatypes.COORDINATE;

/** Serializable sidecar owned by one production-room instance. */
public final class V7144ProductionStorageState implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final int CURRENT_SCHEMA_VERSION = 1;
	private int schemaVersion;
	private short[] xs;
	private short[] ys;
	private MultiResourceCell[] cells;
	private transient boolean[] telemetryBound;

	private V7144ProductionStorageState(short[] xs, short[] ys, MultiResourceCell[] cells) {
		this.xs = xs; this.ys = ys; this.cells = cells;
		this.schemaVersion = CURRENT_SCHEMA_VERSION;
		this.telemetryBound = new boolean[cells.length];
		java.util.Arrays.fill(this.telemetryBound, true);
	}

	/** Creates new state or migrates the original packed primary-output amounts from room data. */
	public static V7144ProductionStorageState capture(RoomInstance instance, int storageTileCode,
			int capacity, String primaryResourceId) {
		V7144MultiResourceStorage.requireReady();
		// Room body iterators reuse one mutable coordinate object. Retain immutable
		// tile indexes, never the iterator object itself.
		ArrayList<Integer> tiles = storageTiles(instance, storageTileCode);
		short[] xs = new short[tiles.size()];
		short[] ys = new short[tiles.size()];
		MultiResourceCell[] cells = new MultiResourceCell[tiles.size()];
		int kinds = MultiResourceStorageRegistry.advancedMaxResourceKinds(instance.blueprint().key);
		int amountMask = amountMask(capacity);
		int shift = Integer.bitCount(amountMask);
		for (int i = 0; i < tiles.size(); i++) {
			int tile = tiles.get(i);
			xs[i] = (short) (tile%SETT.TWIDTH);
			ys[i] = (short) (tile/SETT.TWIDTH);
			cells[i] = new MultiResourceCell(capacity, kinds);
			int legacy = SETT.ROOMS().data.get(tile);
			int amount = legacy & amountMask;
			int reserved = (legacy >>> shift) & amountMask;
			if (primaryResourceId != null && (amount > 0 || reserved > 0))
				cells[i].importEntry(primaryResourceId, amount, Math.min(reserved, amount), 0);
			if (legacy != 0) SETT.ROOMS().data.set(instance, tile, 0);
			MultiResourceStorageRegistry.cellCreated(cells[i].totalAmount(), cells[i].totalPickupReserved(), 0);
		}
		return new V7144ProductionStorageState(xs, ys, cells);
	}

	public MultiResourceCell cell(int tx, int ty) {
		for (int i = 0; i < cells.length; i++) if (xs[i] == tx && ys[i] == ty) return cells[i];
		return null;
	}

	public void bindCell(int tx, int ty) {
		if (telemetryBound == null || telemetryBound.length != cells.length)
			telemetryBound = new boolean[cells.length];
		for (int i = 0; i < cells.length; i++) {
			if (xs[i] != tx || ys[i] != ty || telemetryBound[i]) continue;
			MultiResourceStorageRegistry.cellCreated(cells[i].totalAmount(),
					cells[i].totalPickupReserved(), cells[i].totalIncomingReserved());
			telemetryBound[i] = true;
			return;
		}
	}

	public int size() { return cells.length; }
	public int x(int index) { return xs[index]; }
	public int y(int index) { return ys[index]; }
	public MultiResourceCell cell(int index) { return cells[index]; }
	public boolean hasSharedRoom() {
		for (MultiResourceCell cell : cells) if (cell.sharedReservable() > 0) return true;
		return false;
	}

	/** Advanced state cannot be retired while it still owns units or reservations. */
	public boolean hasRetainedState() {
		for (MultiResourceCell cell : cells)
			if (cell.totalAmount() > 0 || cell.totalPickupReserved() > 0
					|| cell.totalIncomingReserved() > 0) return true;
		return false;
	}

	/** Releases telemetry for an empty sidecar before its owner returns to vanilla storage. */
	public void releaseEmpty() {
		if (hasRetainedState())
			throw new IllegalStateException("Production storage still owns resources or reservations.");
		if (telemetryBound == null) return;
		for (int i = 0; i < cells.length; i++) {
			if (!telemetryBound[i]) continue;
			MultiResourceStorageRegistry.cellDisposed(0, 0, 0);
			telemetryBound[i] = false;
		}
	}

	public void configure(RoomInstance instance, int storageTileCode, int capacity, String family) {
		V7144MultiResourceStorage.requireReady();
		if (schemaVersion < 0 || schemaVersion > CURRENT_SCHEMA_VERSION)
			throw new IllegalStateException("Unsupported production-storage save schema: " + schemaVersion);
		int savedSchema = schemaVersion;
		int savedCells = cells.length;
		int kinds = MultiResourceStorageRegistry.advancedMaxResourceKinds(instance.blueprint().key);
		reconcileCoordinates(instance, storageTileCode, capacity, kinds, family);
		if (telemetryBound == null || telemetryBound.length != cells.length)
			telemetryBound = new boolean[cells.length];
		for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
			MultiResourceCell cell = cells[cellIndex];
			cell.configure(Math.max(capacity, cell.totalAmount()+cell.totalIncomingReserved()),
					Math.max(kinds, cell.entryCount()));
			for (int entry = 0; entry < cell.entryCount(); entry++)
				if (V7144MultiResourceStorage.resource(cell.resourceIdAt(entry)) == null)
					throw new IllegalStateException("Saved production-storage resource is unavailable: " + cell.resourceIdAt(entry));
			if (!telemetryBound[cellIndex]) MultiResourceStorageRegistry.cellCreated(
					cell.totalAmount(), cell.totalPickupReserved(), cell.totalIncomingReserved());
			telemetryBound[cellIndex] = true;
		}
		schemaVersion = CURRENT_SCHEMA_VERSION;
		if (savedSchema != schemaVersion || savedCells != cells.length)
			V7144ProductionOutputDiagnostics.sidecarMigrated(family, instance,
					savedSchema, schemaVersion, savedCells, cells.length);
	}

	/** Repairs sidecars created while the mutable room-body iterator was retained by reference. */
	private void reconcileCoordinates(RoomInstance instance, int storageTileCode,
			int capacity, int kinds, String family) {
		ArrayList<Integer> tiles = storageTiles(instance, storageTileCode);
		resizeForLayout(tiles.size(), capacity, kinds);
		int changed = 0;
		for (int i = 0; i < cells.length; i++) {
			int tile = tiles.get(i);
			short actualX = (short) (tile%SETT.TWIDTH);
			short actualY = (short) (tile/SETT.TWIDTH);
			if (xs[i] == actualX && ys[i] == actualY) continue;
			xs[i] = actualX;
			ys[i] = actualY;
			changed++;
		}
		if (changed > 0)
			V7144ProductionOutputDiagnostics.coordinatesReconciled(
					family, instance, changed, cells.length);
	}

	/**
	 * The V71.44 loader invokes Room.loadFix() before placing the deserialized room
	 * object in RoomsMap.rooms[]. RoomInstance.is(...) therefore returns false for
	 * every tile during migration even though the raw room-index map is already
	 * loaded. Membership must be derived from that stable index map instead.
	 */
	private static ArrayList<Integer> storageTiles(RoomInstance instance, int storageTileCode) {
		ArrayList<Integer> tiles = new ArrayList<Integer>();
		for (COORDINATE coordinate : instance.body()) {
			int tile = coordinate.x() + coordinate.y()*SETT.TWIDTH;
			if (SETT.ROOMS().map.indexGetter.get(tile) == instance.index()
					&& SETT.ROOMS().fData.tileData.get(coordinate) == storageTileCode)
				tiles.add(tile);
		}
		return tiles;
	}

	/** Evolves empty layout tails without discarding saved resources or reservations. */
	private void resizeForLayout(int actualCells, int capacity, int kinds) {
		if (actualCells == cells.length) return;
		if (actualCells <= 0)
			throw new IllegalStateException("Production room has no storage tiles after room-index restoration.");
		if (actualCells < cells.length) {
			for (int i = actualCells; i < cells.length; i++)
				if (retained(cells[i]))
					throw new IllegalStateException("Production storage layout removed a non-empty saved cell: index=" + i);
			if (telemetryBound != null)
				for (int i = actualCells; i < Math.min(cells.length, telemetryBound.length); i++)
					if (telemetryBound[i]) MultiResourceStorageRegistry.cellDisposed(0, 0, 0);
		}
		int oldLength = cells.length;
		xs = Arrays.copyOf(xs, actualCells);
		ys = Arrays.copyOf(ys, actualCells);
		cells = Arrays.copyOf(cells, actualCells);
		boolean[] oldTelemetry = telemetryBound;
		telemetryBound = oldTelemetry == null ? new boolean[actualCells]
				: Arrays.copyOf(oldTelemetry, actualCells);
		for (int i = oldLength; i < actualCells; i++)
			cells[i] = new MultiResourceCell(capacity, kinds);
	}

	private static boolean retained(MultiResourceCell cell) {
		return cell.totalAmount() > 0 || cell.totalPickupReserved() > 0
				|| cell.totalIncomingReserved() > 0;
	}

	public void unbindCell(int tx, int ty) {
		if (telemetryBound == null) return;
		for (int i = 0; i < cells.length; i++) {
			if (xs[i] != tx || ys[i] != ty || !telemetryBound[i]) continue;
			MultiResourceStorageRegistry.cellDisposed(cells[i].totalAmount(),
					cells[i].totalPickupReserved(), cells[i].totalIncomingReserved());
			telemetryBound[i] = false;
			return;
		}
	}

	private static int amountMask(int maximum) {
		int value = maximum;
		int mask = 0;
		while (value != 0) { value /= 2; mask = (mask << 1) | 1; }
		return mask;
	}
}
