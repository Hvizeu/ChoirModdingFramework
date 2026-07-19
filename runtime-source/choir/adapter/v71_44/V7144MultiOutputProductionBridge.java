package choir.adapter.v71_44;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import choir.api.production.MultiOutputRoomDeclaration;
import choir.api.production.ProductionRoomFamily;
import choir.internal.ChoirDiagnostics;
import choir.internal.production.MultiOutputProductionRegistry;
import init.resources.RESOURCE;
import settlement.entity.humanoid.Humanoid;
import settlement.main.SETT;
import settlement.room.industry.module.Industry;
import settlement.room.industry.module.IndustryResource;
import settlement.room.industry.module.ROOM_PRODUCER_INSTANCE;
import settlement.room.industry.refiner.ROOM_REFINER;
import settlement.room.industry.workshop.ROOM_WORKSHOP;
import settlement.room.main.RoomBlueprintImp;
import settlement.room.main.RoomInstance;
import snake2d.util.datatypes.COORDINATE;

/** Version-sensitive physical execution bridge. No vanilla type crosses the public API. */
public final class V7144MultiOutputProductionBridge {
	private static final IdentityHashMap<Industry, LivePlan> livePlans = new IdentityHashMap<Industry, LivePlan>();
	private static volatile boolean ready;

	private V7144MultiOutputProductionBridge() { }

	public static synchronized void initialize() {
		V7144ProductionTargetFingerprint.Result result = V7144ProductionTargetFingerprint.verify();
		ready = result.matches;
		MultiOutputProductionRegistry.adapterReady(ready, result.detail());
		ChoirDiagnostics.info("MULTI-OUTPUT compatibility-fingerprint matched=" + result.matches
				+ " gameJar=" + result.jar + " actual=" + result.actual);
	}

	/** Called exactly once after vanilla has worked and stored output slot zero. */
	public static void afterPrimaryOutput(RoomBlueprintImp blueprint, ROOM_PRODUCER_INSTANCE instance,
			Humanoid worker, COORDINATE workTile, double workSeconds) {
		if (!ready || blueprint == null || instance == null || worker == null || workTile == null) return;
		try {
			Industry industry = instance.industry();
			long generation = MultiOutputProductionRegistry.generation();
			LivePlan live;
			synchronized (V7144MultiOutputProductionBridge.class) {
				live = livePlans.get(industry);
				if (live == null || live.registrationGeneration != generation) {
					live = resolve(blueprint, instance, industry, generation);
					livePlans.put(industry, live);
				}
			}
			if (live.declaration == null) return;
			int stacks = 0;
			int units = 0;
			for (int index = 1; index < live.outputCount; index++) {
				IndustryResource output = industry.outs().get(index);
				int amount = output.work(worker, instance, workSeconds);
				if (amount <= 0) continue;
				int remaining;
				try {
					remaining = storeInsideRoom(instance, output.resource, amount);
				} catch (Throwable storageFailure) {
					MultiOutputProductionRegistry.recordFailure("storage "
							+ storageFailure.getClass().getSimpleName() + ": " + storageFailure.getMessage());
					remaining = amount;
				}
				if (remaining > 0) SETT.THINGS().resources.create(workTile, output.resource, remaining);
				stacks++;
				units += amount;
			}
			MultiOutputProductionRegistry.recordEmission(live.declaration, stacks, units);
		} catch (Throwable failure) {
			MultiOutputProductionRegistry.recordFailure(failure.getClass().getSimpleName() + ": " + failure.getMessage());
		}
	}

	private static int storeInsideRoom(ROOM_PRODUCER_INSTANCE producer, RESOURCE resource, int amount) {
		if (!(producer instanceof RoomInstance) || !(producer instanceof V7144ProductionStorageOwner)) return amount;
		if (!((V7144ProductionStorageOwner) producer).choirUsesAdvancedProductionStorage()) return amount;
		V7144ProductionStorageState state = ((V7144ProductionStorageOwner) producer).choirProductionStorage();
		RoomInstance room = (RoomInstance) producer;
		if (state == null) return amount;
		int remaining = amount;
		for (int i = 0; i < state.size() && remaining > 0; i++) {
			settlement.misc.util.RESOURCE_TILE endpoint = room.resourceTile(state.x(i), state.y(i));
			if (endpoint instanceof V7144ProductionRoomStorage)
				remaining -= ((V7144ProductionRoomStorage) endpoint).deposit(resource, remaining);
		}
		return remaining;
	}

	public static synchronized void disposed() {
		livePlans.clear();
		MultiOutputProductionRegistry.disposeRuntime();
	}

	private static LivePlan resolve(RoomBlueprintImp blueprint, ROOM_PRODUCER_INSTANCE instance,
			Industry industry, long generation) {
		ProductionRoomFamily family;
		if (blueprint instanceof ROOM_WORKSHOP) family = ProductionRoomFamily.WORKSHOP;
		else if (blueprint instanceof ROOM_REFINER) family = ProductionRoomFamily.REFINER;
		else return LivePlan.disabled(generation);
		ArrayList<String> inputs = new ArrayList<String>();
		for (IndustryResource input : industry.ins()) inputs.add(publicResourceId(input.resource));
		ArrayList<String> outputs = new ArrayList<String>();
		for (IndustryResource output : industry.outs()) outputs.add(publicResourceId(output.resource));
		MultiOutputRoomDeclaration declaration = MultiOutputProductionRegistry.resolve(
				blueprint.key, instance.industryI(), family, List.copyOf(inputs), List.copyOf(outputs));
		return declaration == null ? LivePlan.disabled(generation)
				: new LivePlan(generation, declaration, outputs.size());
	}

	private static String publicResourceId(RESOURCE resource) {
		String key = resource.key;
		if ("_STONE".equals(key) || "_WOOD".equals(key) || "_LIVESTOCK".equals(key)) return key.substring(1);
		return key;
	}

	private static final class LivePlan {
		final long registrationGeneration;
		final MultiOutputRoomDeclaration declaration;
		final int outputCount;
		LivePlan(long registrationGeneration, MultiOutputRoomDeclaration declaration, int outputCount) {
			this.registrationGeneration = registrationGeneration; this.declaration = declaration; this.outputCount = outputCount;
		}
		static LivePlan disabled(long generation) { return new LivePlan(generation, null, 0); }
	}
}
