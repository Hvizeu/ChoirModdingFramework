package choir.adapter.v71_44;

import java.util.ArrayList;
import java.util.List;

import choir.api.experimental.resources.ResourceDisplayEffectiveSnapshot;
import choir.internal.ChoirDiagnostics;
import choir.internal.resources.ResourceDisplayRuntime;
import choir.internal.resources.ResourceDisplayStockpileSelectorPlan;
import choir.internal.resources.ResourceDisplayStockpileSelectorPlan.SelectorPlan;
import init.resources.RESOURCE;

/** Adapter-private, construction-time bridge for the V71.44 stockpile selector shadow. */
public final class V7144ResourceDisplayStockpileBridge {
	private static V7144ResourceDisplayResolver resolver;
	private static boolean targetFingerprintsCompatible;

	private V7144ResourceDisplayStockpileBridge() { }

	static synchronized void initialize(V7144ResourceDisplayResolver value, boolean compatible) {
		resolver = value;
		targetFingerprintsCompatible = compatible;
	}

	public static void shadowConstructed() {
		ChoirDiagnostics.info("RESOURCE-DISPLAY stockpile-selector shadow-active"
				+ " class=settlement.room.infra.stockpile.Gui"
				+ " targetFingerprintsCompatible=" + targetFingerprintsCompatible);
	}

	public static Presentation presentation() {
		try {
			ResourceDisplayEffectiveSnapshot snapshot = ResourceDisplayRuntime.snapshot();
			V7144ResourceDisplayResolver currentResolver;
			synchronized (V7144ResourceDisplayStockpileBridge.class) { currentResolver = resolver; }
			SelectorPlan plan = ResourceDisplayStockpileSelectorPlan.build(snapshot,
					groupId -> ResourceDisplayRuntime.currentHandlesForGroup(groupId), 2);
			if (currentResolver == null || !targetFingerprintsCompatible || !plan.named()) {
				String reason = currentResolver == null ? "adapter bridge unavailable"
						: (!targetFingerprintsCompatible ? "target fingerprints incompatible" : plan.reason());
				return Presentation.vanilla(reason);
			}

			List<Group> groups = new ArrayList<Group>(plan.groups().size());
			for (ResourceDisplayStockpileSelectorPlan.Group group : plan.groups()) {
				List<RESOURCE> resources = new ArrayList<RESOURCE>(group.handles().size());
				for (var handle : group.handles()) {
					RESOURCE resource = currentResolver.liveResource(handle);
					if (resource.index() != handle.observedIndex()
							|| resource.category != handle.nativeCategory())
						throw new IllegalStateException("Live resource identity changed for " + handle.resourceId());
					resources.add(resource);
				}
				groups.add(new Group(group.groupId(), group.label(), group.fallback(), resources, group.resourceRows()));
			}
			return Presentation.named(groups, plan);
		} catch (Throwable failure) {
			ChoirDiagnostics.error("RESOURCE-DISPLAY stockpile-selector failure phase=bridge"
					+ " reason=" + failure.getClass().getSimpleName() + ":" + failure.getMessage());
			return Presentation.vanilla("bridge exception");
		}
	}

	public static void constructed(Presentation value, int toggleRows, int headerRows, int stateBindingFailures) {
		if (value.named() && (toggleRows != value.resourceCount() || headerRows != value.groupCount()
				|| stateBindingFailures != 0))
			throw new IllegalStateException("Stockpile selector construction count mismatch");
		ChoirDiagnostics.info("RESOURCE-DISPLAY stockpile-selector mode="
				+ (value.named() ? "named-groups" : "vanilla-fallback")
				+ " runtime.generation=" + value.runtimeGeneration()
				+ " model.generation=" + value.modelGeneration()
				+ " model.signature=" + (value.modelSignature().isEmpty() ? "<none>" : value.modelSignature())
				+ " groups=" + value.groupCount()
				+ " explicit.groups=" + value.explicitGroupCount()
				+ " fallback.groups=" + value.fallbackGroupCount()
				+ " resources=" + value.resourceCount()
				+ " fallback.resources=" + value.fallbackResourceCount()
				+ " toggle.rows=" + toggleRows
				+ " header.rows=" + headerRows
				+ " duplicate.rows=0 missing.rows=0 state-binding.failures=" + stateBindingFailures
				+ " reason=" + value.reason().replace(' ', '_'));
	}

	public static void constructionFailure(Throwable failure) {
		ChoirDiagnostics.error("RESOURCE-DISPLAY stockpile-selector failure phase=layout"
				+ " reason=" + failure.getClass().getSimpleName() + ":" + failure.getMessage());
	}

	public static void fallbackConstructed(String reason, int toggleRows) {
		constructed(Presentation.vanilla(reason), toggleRows, 0, 0);
	}

	public static final class Group {
		private final String groupId;
		private final String label;
		private final boolean fallback;
		private final List<RESOURCE> resources;
		private final int resourceRows;

		private Group(String groupId, String label, boolean fallback, List<RESOURCE> resources, int resourceRows) {
			this.groupId = groupId;
			this.label = label;
			this.fallback = fallback;
			this.resources = List.copyOf(resources);
			this.resourceRows = resourceRows;
		}

		public String groupId() { return groupId; }
		public String label() { return label; }
		public boolean fallback() { return fallback; }
		public List<RESOURCE> resources() { return resources; }
		public int resourceRows() { return resourceRows; }
	}

	public static final class Presentation {
		private final boolean named;
		private final String reason;
		private final List<Group> groups;
		private final long runtimeGeneration;
		private final long modelGeneration;
		private final String modelSignature;
		private final int fallbackGroupCount;
		private final int resourceCount;
		private final int fallbackResourceCount;

		private Presentation(boolean named, String reason, List<Group> groups, long runtimeGeneration,
				long modelGeneration, String modelSignature, int fallbackGroupCount,
				int resourceCount, int fallbackResourceCount) {
			this.named = named;
			this.reason = reason;
			this.groups = List.copyOf(groups);
			this.runtimeGeneration = runtimeGeneration;
			this.modelGeneration = modelGeneration;
			this.modelSignature = modelSignature;
			this.fallbackGroupCount = fallbackGroupCount;
			this.resourceCount = resourceCount;
			this.fallbackResourceCount = fallbackResourceCount;
		}

		static Presentation vanilla(String reason) {
			return new Presentation(false, reason, List.of(), 0, 0, "", 0, 0, 0);
		}
		static Presentation named(List<Group> groups, SelectorPlan plan) {
			return new Presentation(true, plan.reason(), groups, plan.runtimeGeneration(), plan.modelGeneration(),
					plan.modelSignature(), plan.fallbackGroupCount(), plan.resourceCount(), plan.fallbackResourceCount());
		}

		public boolean named() { return named; }
		public String reason() { return reason; }
		public List<Group> groups() { return groups; }
		public long runtimeGeneration() { return runtimeGeneration; }
		public long modelGeneration() { return modelGeneration; }
		public String modelSignature() { return modelSignature; }
		public int groupCount() { return groups.size(); }
		public int explicitGroupCount() { return groups.size() - fallbackGroupCount; }
		public int fallbackGroupCount() { return fallbackGroupCount; }
		public int resourceCount() { return resourceCount; }
		public int fallbackResourceCount() { return fallbackResourceCount; }
	}
}
