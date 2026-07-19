package choir.adapter.v71_44;

import java.util.concurrent.ConcurrentHashMap;

import choir.internal.storage.MultiResourceStorageRegistry;
import choir.api.storage.ChoirStorage;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import init.resources.RBIT;
import settlement.misc.util.RESOURCE_TILE;
import settlement.misc.util.TILE_STORAGE;
import settlement.room.infra.stockpile.StockpileInstance;
import settlement.room.main.Room;
import settlement.room.main.RoomInstance;
import settlement.room.main.job.StorageCrate;
import snake2d.util.datatypes.COORDINATE;

/** Internal V71.44 resource identity and room-policy adapter. */
public final class V7144MultiResourceStorage {
	private static volatile boolean ready;
	private static final ConcurrentHashMap<StockpileInstance, Boolean> stockpileModes =
			new ConcurrentHashMap<StockpileInstance, Boolean>();
	private static final ThreadLocal<Preferred> preferred = new ThreadLocal<Preferred>() {
		@Override protected Preferred initialValue() { return new Preferred(); }
	};
	private V7144MultiResourceStorage() { }

	public static synchronized void initialize() {
		V7144StorageTargetFingerprint.Result result = V7144StorageTargetFingerprint.verify();
		ready = result.matches;
		MultiResourceStorageRegistry.adapterReady(ready, result.detail);
		choir.internal.ChoirDiagnostics.info("MULTI-STORAGE compatibility-fingerprints matched=" + result.matches
				+ " targets=" + V7144StorageTargetFingerprint.targetCount()
				+ " gameJar=" + result.jar + " detail=" + result.detail);
	}

	public static void requireReady() {
		if (!ready) throw new IllegalStateException("Choir multi-resource storage is unavailable because the exact V71.44 target fingerprints did not match.");
	}

	public static synchronized void disposed() {
		preferred.remove();
		stockpileModes.clear();
		MultiResourceStorageRegistry.disposeRuntime();
	}

	/** Records a room-local mode without widening the exact vanilla public class surface. */
	public static void rememberStockpileMode(StockpileInstance instance, boolean advanced) {
		if (instance != null) stockpileModes.put(instance, Boolean.valueOf(advanced));
	}

	public static void forgetStockpileMode(StockpileInstance instance) {
		if (instance != null) stockpileModes.remove(instance);
	}

	private static boolean stockpileAdvanced(StockpileInstance instance) {
		Boolean value = stockpileModes.get(instance);
		return value != null ? value.booleanValue() : policyAdvanced(instance);
	}

	public static void rememberPreferred(int tx, int ty, RESOURCE resource) {
		Preferred value = preferred.get();
		value.tx = tx; value.ty = ty; value.resource = resource;
	}

	public static RESOURCE preferred(int tx, int ty) {
		Preferred value = preferred.get();
		return value.tx == tx && value.ty == ty ? value.resource : null;
	}

	public static void clearPreferred(int tx, int ty) {
		Preferred value = preferred.get();
		if (value.tx == tx && value.ty == ty) {
			value.tx = Integer.MIN_VALUE; value.ty = Integer.MIN_VALUE; value.resource = null;
		}
	}

	public static String stableResourceId(RESOURCE resource) {
		if (resource == null) throw new IllegalArgumentException("Resource is required.");
		String key = resource.key;
		if ("_STONE".equals(key) || "_WOOD".equals(key) || "_LIVESTOCK".equals(key)) return key.substring(1);
		return key;
	}

	public static RESOURCE resource(String stableId) {
		if (stableId == null) return null;
		for (RESOURCE resource : RESOURCES.ALL())
			if (stableId.equals(stableResourceId(resource))) return resource;
		return null;
	}

	public static int maxResourceKinds(Room room) {
		String roomKey = room == null || room.blueprint() == null ? "" : room.blueprint().key;
		if (room instanceof StockpileInstance) {
			return stockpileAdvanced((StockpileInstance) room)
					? MultiResourceStorageRegistry.advancedMaxResourceKinds(roomKey)
					: ChoirStorage.VANILLA_MAX_RESOURCE_KINDS;
		}
		if (room instanceof V7144ProductionStorageOwner) {
			return ((V7144ProductionStorageOwner) room).choirUsesAdvancedProductionStorage()
					? MultiResourceStorageRegistry.advancedMaxResourceKinds(roomKey)
					: ChoirStorage.VANILLA_MAX_RESOURCE_KINDS;
		}
		return MultiResourceStorageRegistry.maxResourceKinds(roomKey);
	}

	/** Startup/world-latched policy used when a new storage-bearing room is constructed. */
	public static boolean policyAdvanced(Room room) {
		String roomKey = room == null || room.blueprint() == null ? "" : room.blueprint().key;
		return MultiResourceStorageRegistry.requiresAdvancedStorage(roomKey);
	}

	/**
	 * V71.44 stockpiles already maintain a room-level accepted-resource mask.
	 * Other specialized storage rooms retain their narrower vanilla assignment policy.
	 */
	public static boolean acceptsAdditional(RoomInstance instance, RESOURCE resource) {
		return isFlexibleStockpile(instance)
				&& resource != null && ((StockpileInstance) instance).crateMask.has(resource);
	}

	public static boolean isFlexibleStockpile(RoomInstance instance) {
		return instance instanceof StockpileInstance
				&& stockpileAdvanced((StockpileInstance) instance);
	}

	/** One physical shelf exposes one storage section per configured resource kind. */
	public static int stockpileAssignmentCapacity(StockpileInstance instance) {
		if (instance == null) return 0;
		long slots = (long) instance.totalCrates() * maxResourceKinds(instance);
		return slots >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) slots;
	}

	public static int acceptedResourceCount(RoomInstance instance) {
		if (!(instance instanceof StockpileInstance)) return 0;
		int count = 0;
		for (RESOURCE resource : RESOURCES.ALL())
			if (((StockpileInstance) instance).crateMask.has(resource)) count++;
		return count;
	}

	public static RESOURCE acceptedResourceAt(RoomInstance instance, int acceptedIndex) {
		if (!(instance instanceof StockpileInstance) || acceptedIndex < 0) return null;
		for (RESOURCE resource : RESOURCES.ALL()) {
			if (!((StockpileInstance) instance).crateMask.has(resource)) continue;
			if (acceptedIndex-- == 0) return resource;
		}
		return null;
	}

	/**
	 * Exact, allocation-free lookup bounded to one room. Stockpiles prefer the
	 * already-used shelf with the least remaining room so partial stacks are
	 * completed before an empty assigned shelf is opened.
	 */
	public static TILE_STORAGE exactDestination(RoomInstance room, TILE_STORAGE candidate,
			RESOURCE resource, int minimum) {
		if (isFlexibleStockpile(room))
			return packedStockpileDestination(room, candidate, resource, minimum);
		TILE_STORAGE exact = candidate == null ? null : candidate.storageFor(resource);
		if (exact != null && exact.storageReservable() >= minimum) return exact;
		for (COORDINATE coordinate : room.body()) {
			if (!room.is(coordinate)) continue;
			TILE_STORAGE physical = room.storage(coordinate.x(), coordinate.y());
			exact = physical == null ? null : physical.storageFor(resource);
			if (exact != null && exact.storageIsFindable() && exact.storageReservable() >= minimum) return exact;
		}
		return null;
	}

	private static TILE_STORAGE packedStockpileDestination(RoomInstance room, TILE_STORAGE candidate,
			RESOURCE resource, int minimum) {
		int bestX = Integer.MIN_VALUE;
		int bestY = Integer.MIN_VALUE;
		int bestUsed = -1;
		int bestRemaining = Integer.MAX_VALUE;
		TILE_STORAGE exact = candidate == null ? null : candidate.storageFor(resource);
		if (usable(exact, minimum)) {
			bestX = exact.x();
			bestY = exact.y();
			bestUsed = assignedAmount(exact, resource);
			bestRemaining = exact.storageReservable();
		}
		for (COORDINATE coordinate : room.body()) {
			if (!room.is(coordinate)) continue;
			TILE_STORAGE physical = room.storage(coordinate.x(), coordinate.y());
			exact = physical == null ? null : physical.storageFor(resource);
			if (!usable(exact, minimum)) continue;
			int used = assignedAmount(exact, resource);
			int remaining = exact.storageReservable();
			boolean better = used > 0 && bestUsed <= 0
					|| (used > 0) == (bestUsed > 0) && remaining < bestRemaining
					|| (used > 0) == (bestUsed > 0) && remaining == bestRemaining && used > bestUsed;
			if (!better) continue;
			bestX = exact.x();
			bestY = exact.y();
			bestUsed = used;
			bestRemaining = remaining;
		}
		if (bestX == Integer.MIN_VALUE) return null;
		TILE_STORAGE physical = room.storage(bestX, bestY);
		return physical == null ? null : physical.storageFor(resource);
	}

	private static boolean usable(TILE_STORAGE storage, int minimum) {
		return storage != null && storage.storageIsFindable() && storage.storageReservable() >= minimum;
	}

	private static int assignedAmount(TILE_STORAGE storage, RESOURCE resource) {
		return storage instanceof StorageCrate ? ((StorageCrate) storage).assignmentAmount(resource) : 0;
	}

	public static TILE_STORAGE exactDestination(RoomInstance room, TILE_STORAGE candidate,
			RBIT mask, int minimum) {
		TILE_STORAGE exact = candidate == null ? null : candidate.storageFor(mask);
		if (exact != null && exact.storageReservable() >= minimum)
			return isFlexibleStockpile(room) && exact.resource() != null
					? exactDestination(room, exact, exact.resource(), minimum) : exact;
		for (COORDINATE coordinate : room.body()) {
			if (!room.is(coordinate)) continue;
			TILE_STORAGE physical = room.storage(coordinate.x(), coordinate.y());
			exact = physical == null ? null : physical.storageFor(mask);
			if (exact != null && exact.storageIsFindable() && exact.storageReservable() >= minimum)
				return isFlexibleStockpile(room) && exact.resource() != null
						? exactDestination(room, exact, exact.resource(), minimum) : exact;
		}
		return null;
	}

	public static RESOURCE_TILE exactSource(RoomInstance room, RESOURCE_TILE candidate,
			RBIT mask, int minimum) {
		RESOURCE_TILE exact = candidate == null ? null : candidate.resourceFor(mask);
		if (exact != null && exact.reservable() >= minimum) return exact;
		for (COORDINATE coordinate : room.body()) {
			if (!room.is(coordinate)) continue;
			RESOURCE_TILE physical = room.resourceTile(coordinate.x(), coordinate.y());
			exact = physical == null ? null : physical.resourceFor(mask);
			if (exact != null && exact.reservable() >= minimum) return exact;
		}
		return null;
	}

	private static final class Preferred {
		int tx = Integer.MIN_VALUE;
		int ty = Integer.MIN_VALUE;
		RESOURCE resource;
	}
}
