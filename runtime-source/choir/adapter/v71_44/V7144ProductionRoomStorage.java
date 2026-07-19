package choir.adapter.v71_44;

import choir.internal.storage.MultiResourceCell;
import choir.internal.storage.MultiResourceStorageRegistry;
import init.resources.RBIT;
import init.resources.RESOURCE;
import settlement.main.SETT;
import settlement.misc.util.RESOURCE_TILE;
import settlement.room.main.ROOMA;
import settlement.room.main.RoomInstance;
import settlement.room.main.job.RoomResStorage;
import snake2d.SPRITE_RENDERER;
import snake2d.util.datatypes.DIR;
import util.rendering.ShadowBatch;

/** Multi-resource implementation used by workshop and refiner output shelves. */
public abstract class V7144ProductionRoomStorage extends RoomResStorage {
	private final int capacity;
	private MultiResourceCell cell;
	private int activeIndex = -1;
	private RESOURCE activeResource;

	protected V7144ProductionRoomStorage(int capacity) {
		super(capacity);
		this.capacity = capacity;
	}

	@Override public V7144ProductionRoomStorage get(int tx, int ty, ROOMA room) {
		if (!(room instanceof RoomInstance) || !(room instanceof V7144ProductionStorageOwner)
				|| !room.is(tx, ty) || !is(tx, ty)) return null;
		if (!((V7144ProductionStorageOwner) room).choirUsesAdvancedProductionStorage()) return null;
		this.ins = room;
		this.x = tx; this.y = ty;
		V7144ProductionStorageState state = ((V7144ProductionStorageOwner) room).choirProductionStorage();
		if (state != null) state.bindCell(tx, ty);
		cell = state == null ? null : state.cell(tx, ty);
		if (cell == null) return null;
		selectDefault();
		return this;
	}

	@Override public RESOURCE resource() { return activeResource; }
	@Override public int resourceCount() { return cell == null ? 0 : cell.entryCount(); }
	@Override public RESOURCE_TILE resourceAt(int index) {
		if (cell == null || index < 0 || index >= cell.entryCount()) return null;
		select(index); return this;
	}
	@Override public RESOURCE_TILE resourceFor(RESOURCE expected) {
		if (cell == null || expected == null) return null;
		int index = cell.indexOf(V7144MultiResourceStorage.stableResourceId(expected));
		if (index < 0 || cell.amountAt(index) <= 0) return null;
		select(index); return this;
	}
	@Override public RESOURCE_TILE resourceFor(RBIT mask) {
		if (cell == null || mask == null) return null;
		for (int i = 0; i < cell.entryCount(); i++) {
			RESOURCE resource = V7144MultiResourceStorage.resource(cell.resourceIdAt(i));
			if (resource != null && mask.has(resource) && cell.pickupReservableAt(i) > 0) { select(i); return this; }
		}
		return null;
	}

	@Override public boolean hasRoom() { return cell != null && cell.sharedReservable() > 0; }
	/** Returns whether any output shelf in the current room still has shared capacity. */
	public boolean roomHasSharedCapacity() {
		if (!(ins instanceof V7144ProductionStorageOwner)) return false;
		if (!((V7144ProductionStorageOwner) ins).choirUsesAdvancedProductionStorage()) return false;
		V7144ProductionStorageState state = ((V7144ProductionStorageOwner) ins).choirProductionStorage();
		return state != null && state.hasSharedRoom();
	}
	@Override public int amount() { return activeIndex < 0 ? 0 : cell.amountAt(activeIndex); }
	/** Keeps absence reporting legal after the last unit becomes unavailable. */
	@Override public boolean isFindable() { return activeResource != null; }
	@Override public int reservable() { return activeIndex < 0 ? 0 : cell.pickupReservableAt(activeIndex); }
	@Override public boolean findableReservedIs() { return activeIndex >= 0 && cell.pickupReservedAt(activeIndex) > 0; }
	@Override public boolean findableReservedCanBe() { return activeIndex >= 0 && cell.pickupReservableAt(activeIndex) > 0; }

	@Override public void findableReserve() {
		if (!findableReservedCanBe()) throw new RuntimeException("No production output available to reserve.");
		boolean before = findableReservedCanBe();
		int reserved = cell.reservePickup(id(), 1);
		MultiResourceStorageRegistry.recordDelta(0, reserved, 0);
		reportSourceTransition(before);
	}
	@Override public void findableReserveCancel() {
		if (!findableReservedIs()) return;
		boolean before = findableReservedCanBe();
		int cancelled = cell.cancelPickup(id(), 1);
		MultiResourceStorageRegistry.recordDelta(0, -cancelled, 0);
		reportSourceTransition(before);
	}
	@Override public void resourcePickup() {
		if (!findableReservedIs()) return;
		boolean before = findableReservedCanBe();
		int picked = cell.pickupReserved(id(), 1);
		MultiResourceStorageRegistry.recordDelta(-picked, -picked, 0);
		reportSourceTransition(before);
		cell.removeEmptyUnpinnedEntry(id());
		selectDefault();
		changed(x, y);
	}

	@Override public void deposit() {
		RESOURCE primary = primaryResource();
		if (primary == null || deposit(primary, 1) != 1) throw new RuntimeException("Production storage is full.");
	}
	@Override public int deposit(int amount) {
		RESOURCE primary = primaryResource();
		return primary == null ? 0 : deposit(primary, amount);
	}

	public int deposit(RESOURCE resource, int amount) {
		if (cell == null || resource == null || amount <= 0) return 0;
		int existing = cell.indexOf(V7144MultiResourceStorage.stableResourceId(resource));
		boolean sourceBefore = existing >= 0 && cell.pickupReservableAt(existing) > 0;
		int deposited = cell.depositDirect(V7144MultiResourceStorage.stableResourceId(resource), amount, true);
		if (deposited > 0) {
			activeIndex = cell.indexOf(V7144MultiResourceStorage.stableResourceId(resource));
			activeResource = resource;
			MultiResourceStorageRegistry.recordDelta(deposited, 0, 0);
			if (!sourceBefore && findableReservedCanBe()) SETT.PATH().finders.resource.reportPresence(this);
			changed(x, y);
		}
		return deposited;
	}

	@Override public void dispose() {
		if (cell == null) return;
		V7144ProductionStorageState state = ins instanceof V7144ProductionStorageOwner
				? ((V7144ProductionStorageOwner) ins).choirProductionStorage() : null;
		for (int i = cell.entryCount()-1; i >= 0; i--) {
			select(i);
			if (findableReservedCanBe()) SETT.PATH().finders.resource.reportAbsence(this);
			RESOURCE resource = activeResource;
			int amount = cell.amountAt(i);
			int pickup = cell.pickupReservedAt(i);
			if (amount > 0) spill(resource, amount);
			cell.cancelPickup(id(), pickup);
			cell.removeUnreserved(id(), amount);
			cell.removeEmptyEntry(id());
			MultiResourceStorageRegistry.recordDelta(-amount, -pickup, 0);
		}
		if (state != null) state.unbindCell(x, y);
		selectDefault();
		changed(x, y);
	}

	@Override public void render(SPRITE_RENDERER renderer, ShadowBatch shadowBatch,
			int tx, int ty, int px, int py, int random) {
		ROOMA room = SETT.ROOMS().map.rooma.get(tx, ty);
		if (get(tx, ty, room) == null) return;
		for (int i = 0; i < cell.entryCount(); i++) {
			select(i);
			int amount = (int) Math.ceil(cell.amountAt(i)/2.0);
			if (amount <= 0 || activeResource == null) continue;
			shadowBatch.setHeight(1).setDistance2Ground(0);
			activeResource.renderLaying(shadowBatch, px, py, random, amount);
			activeResource.renderLaying(renderer, px, py, random, amount);
			random >>= 3;
		}
	}

	@Override public int max() { return capacity; }

	protected abstract RESOURCE primaryResource();

	private void reportSourceTransition(boolean before) {
		boolean after = findableReservedCanBe();
		if (before && !after) SETT.PATH().finders.resource.reportAbsence(this);
		else if (!before && after) SETT.PATH().finders.resource.reportPresence(this);
	}

	private void spill(RESOURCE resource, int amount) {
		for (DIR direction : DIR.ALL) {
			int dx = x + direction.x(), dy = y + direction.y();
			if (SETT.PATH().connectivity.is(dx, dy)) {
				SETT.THINGS().resources.create(dx, dy, resource, amount); return;
			}
		}
		SETT.THINGS().resources.create(x, y, resource, amount);
	}

	private void selectDefault() {
		if (cell == null || cell.entryCount() == 0) { activeIndex = -1; activeResource = primaryResource(); }
		else select(0);
	}
	private void select(int index) {
		activeIndex = index;
		activeResource = V7144MultiResourceStorage.resource(cell.resourceIdAt(index));
	}
	private String id() { return V7144MultiResourceStorage.stableResourceId(activeResource); }
}
