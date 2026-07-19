package choir.adapter.v71_44;

import choir.internal.diagnostics.DecisionDiagnostics;
import init.resources.RBIT;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import settlement.main.SETT;
import settlement.misc.util.RESOURCE_TILE;
import settlement.room.industry.module.ROOM_PRODUCER_INSTANCE;
import settlement.room.main.RoomInstance;
import snake2d.util.datatypes.COORDINATE;

/** V71.44 production-input audit and safe component-index repair. */
public final class V7144WorkInputDiagnostics {
	private V7144WorkInputDiagnostics() { }

	/** Called at the exact point where WorkAbs reports that its fetch plan returned null. */
	public static void resourceMissing(String family, RoomInstance room, RBIT requested,
			int jobX, int jobY) {
		if (room == null || requested == null) return;
		for (RESOURCE resource : RESOURCES.ALL()) {
			if (!requested.has(resource)) continue;
			String resourceId = V7144MultiResourceStorage.stableResourceId(resource);
			String roomKey = room.blueprintI().key;
			int industry = room instanceof ROOM_PRODUCER_INSTANCE
					? ((ROOM_PRODUCER_INSTANCE) room).industryI() : -1;
			String stableKey = family + '/' + roomKey + '/' + industry + '/' + jobX + ',' + jobY
					+ '/' + resourceId;
			int attempts = DecisionDiagnostics.permitProblem(stableKey);
			if (attempts == 0) continue;
			Audit audit = auditAndRepair(resource, jobX, jobY);
			DecisionDiagnostics.problem("WORK_INPUT_STALLED", attempts,
					"family=" + family + " room=" + roomKey + " industry=" + industry
					+ " job=" + jobX + ',' + jobY + " resource=" + resourceId
					+ " reason=" + audit.reason + " stockpile.amount=" + audit.stockpileAmount
					+ " stockpile.reservable=" + audit.stockpileReservable
					+ " stockpile.capacity=" + audit.stockpileCapacity
					+ " exact.sources=" + audit.exactSources + " exact.amount=" + audit.exactAmount
					+ " exact.reservable=" + audit.exactReservable + " exact.hidden=" + audit.hiddenSources
					+ " nearest.source=" + audit.nearestSourceX + ',' + audit.nearestSourceY
					+ " nearest.direct-distance=" + audit.nearestDirectDistance
					+ " component.stored.before=" + audit.storedBefore
					+ " component.priority.before=" + audit.priorityBefore
					+ " component.scattered.before=" + audit.scatteredBefore
					+ " component.stored.after=" + audit.storedAfter
					+ " component.priority.after=" + audit.priorityAfter
					+ " component.scattered.after=" + audit.scatteredAfter
					+ " component.repairs=" + audit.componentRepairs
					+ " component.refreshes=" + audit.componentRefreshes);
		}
	}

	/** Called at the exact successful counterpart to resourceMissing. */
	public static void resourceFound(String family, RoomInstance room, RESOURCE resource) {
		if (room == null || resource == null) return;
		String roomKey = room.blueprintI().key;
		int industry = room instanceof ROOM_PRODUCER_INSTANCE
				? ((ROOM_PRODUCER_INSTANCE) room).industryI() : -1;
		String resourceId = V7144MultiResourceStorage.stableResourceId(resource);
		int attempts = DecisionDiagnostics.permitDetail("work-input-found/" + family + '/'
				+ roomKey + '/' + industry + '/' + resourceId);
		DecisionDiagnostics.detail("WORK_INPUT_FOUND", attempts,
				"family=" + family + " room=" + roomKey + " industry=" + industry
				+ " resource=" + resourceId);
	}

	/** Detailed, rate-limited endpoint reservation/pickup trace. */
	public static void endpointOperation(String operation, RESOURCE resource, int tx, int ty,
			int requested, int completed) {
		if (resource == null) return;
		String resourceId = V7144MultiResourceStorage.stableResourceId(resource);
		int attempts = DecisionDiagnostics.permitDetail("endpoint/" + operation + '/'
				+ resourceId + '/' + tx + ',' + ty);
		DecisionDiagnostics.detail("RESOURCE_ENDPOINT_" + operation.toUpperCase(), attempts,
				"resource=" + resourceId + " tile=" + tx + ',' + ty
				+ " requested=" + requested + " completed=" + completed);
	}

	private static Audit auditAndRepair(RESOURCE resource, int jobX, int jobY) {
		Audit audit = new Audit();
		audit.stockpileAmount = SETT.ROOMS().STOCKPILE.tally().amountTotal(resource);
		audit.stockpileReservable = SETT.ROOMS().STOCKPILE.tally().amountReservable.get(resource);
		audit.stockpileCapacity = SETT.ROOMS().STOCKPILE.tally().space.total(resource);
		audit.storedBefore = SETT.PATH().comps.data.resCrate.has(jobX, jobY, resource.bit);
		audit.priorityBefore = SETT.PATH().comps.data.resPriority.has(jobX, jobY, resource.bit);
		audit.scatteredBefore = SETT.PATH().comps.data.resScattered.has(jobX, jobY, resource.bit);

		for (int instanceIndex = 0; instanceIndex < SETT.ROOMS().STOCKPILE.instancesSize(); instanceIndex++) {
			RoomInstance stockpile = SETT.ROOMS().STOCKPILE.getInstance(instanceIndex);
			if (stockpile == null) continue;
			for (COORDINATE coordinate : stockpile.body()) {
				if (!stockpile.is(coordinate)) continue;
				RESOURCE_TILE physical = stockpile.resourceTile(coordinate.x(), coordinate.y());
				if (physical == null) continue;
				for (int index = 0; index < physical.resourceCount(); index++) {
					RESOURCE_TILE view = physical.resourceAt(index);
					if (view == null || view.resource() != resource) continue;
					audit.exactSources++;
					audit.exactAmount += view.amount();
					audit.exactReservable += Math.max(0, view.reservable());
					int directDistance = Math.max(Math.abs(view.x()-jobX), Math.abs(view.y()-jobY));
					if (directDistance < audit.nearestDirectDistance) {
						audit.nearestDirectDistance = directDistance;
						audit.nearestSourceX = view.x();
						audit.nearestSourceY = view.y();
					}
					if (!view.isFindable()) { audit.hiddenSources++; continue; }
					if (!view.findableReservedCanBe()) continue;
					audit.eligibleSources++;
					if (audit.firstEligibleX == Integer.MIN_VALUE) {
						audit.firstEligibleX = view.x();
						audit.firstEligibleY = view.y();
					}
					if (!publishedAtSource(view, resource)) {
						SETT.PATH().finders.resource.reportPresence(view);
						audit.componentRepairs++;
					}
				}
			}
		}
		if (audit.exactSources == 0) audit.nearestDirectDistance = -1;
		// Multi-resource tiles contribute several logical sources to one physical
		// component. A failed real worker query is a safe, rare reconciliation
		// point: rebuild the affected component from the exact views instead of
		// trusting a stale positive or negative aggregate bit.
		if (audit.firstEligibleX != Integer.MIN_VALUE) {
			SETT.PATH().comps.updateService(audit.firstEligibleX, audit.firstEligibleY);
			audit.componentRefreshes = 1;
		}

		audit.storedAfter = SETT.PATH().comps.data.resCrate.has(jobX, jobY, resource.bit);
		audit.priorityAfter = SETT.PATH().comps.data.resPriority.has(jobX, jobY, resource.bit);
		audit.scatteredAfter = SETT.PATH().comps.data.resScattered.has(jobX, jobY, resource.bit);
		boolean beforeVisible = audit.storedBefore || audit.priorityBefore || audit.scatteredBefore;
		boolean afterVisible = audit.storedAfter || audit.priorityAfter || audit.scatteredAfter;
		if (audit.exactSources == 0)
			audit.reason = audit.stockpileAmount > 0 ? "SOURCE_VIEW_MISSING" : "NO_STOCKPILE_SOURCE";
		else if (audit.eligibleSources == 0)
			audit.reason = audit.hiddenSources == audit.exactSources ? "SOURCE_HIDDEN"
					: audit.exactReservable == 0 ? "FULLY_RESERVED" : "SOURCE_NOT_FINDABLE";
		else if (!beforeVisible && afterVisible && audit.componentRepairs > 0)
			audit.reason = "COMPONENT_INDEX_REPAIRED";
		else if (!afterVisible)
			audit.reason = "NO_REACHABLE_SOURCE";
		else
			audit.reason = "PATH_OR_ENDPOINT_MISMATCH";
		return audit;
	}

	private static boolean publishedAtSource(RESOURCE_TILE view, RESOURCE resource) {
		if (view.isPrio()) return SETT.PATH().comps.data.resPriority.has(view.x(), view.y(), resource.bit);
		if (view.isStorage()) return SETT.PATH().comps.data.resCrate.has(view.x(), view.y(), resource.bit);
		return SETT.PATH().comps.data.resScattered.has(view.x(), view.y(), resource.bit);
	}

	private static final class Audit {
		int stockpileAmount;
		int stockpileReservable;
		int stockpileCapacity;
		int exactSources;
		int exactAmount;
		int exactReservable;
		int eligibleSources;
		int hiddenSources;
		int componentRepairs;
		int componentRefreshes;
		int nearestSourceX = -1;
		int nearestSourceY = -1;
		int nearestDirectDistance = Integer.MAX_VALUE;
		int firstEligibleX = Integer.MIN_VALUE;
		int firstEligibleY = Integer.MIN_VALUE;
		boolean storedBefore;
		boolean priorityBefore;
		boolean scatteredBefore;
		boolean storedAfter;
		boolean priorityAfter;
		boolean scatteredAfter;
		String reason;
	}
}
