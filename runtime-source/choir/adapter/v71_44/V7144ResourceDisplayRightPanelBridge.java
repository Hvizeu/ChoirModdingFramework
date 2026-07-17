package choir.adapter.v71_44;

import java.util.ArrayList;
import java.util.List;

import choir.api.experimental.resources.ResourceDisplayEffectiveSnapshot;
import choir.internal.ChoirDiagnostics;
import choir.internal.resources.ResourceDisplayRightPanelPlan;
import choir.internal.resources.ResourceDisplayRightPanelPlan.Plan;
import choir.internal.resources.ResourceDisplayRuntime;
import init.resources.RESOURCE;

/** Adapter-private conversion from the stable effective model to current V71.44 UI rows. */
public final class V7144ResourceDisplayRightPanelBridge {
	private static V7144ResourceDisplayResolver resolver;
	private static boolean targetFingerprintsCompatible;

	private V7144ResourceDisplayRightPanelBridge() { }

	static synchronized void initialize(V7144ResourceDisplayResolver value, boolean compatible) {
		resolver = value;
		targetFingerprintsCompatible = compatible;
	}

	public static void shadowConstructed() {
		ChoirDiagnostics.info("RESOURCE-DISPLAY right-panel shadow-active class=view.sett.ui.right.UIMiniResources"
				+ " targetFingerprintsCompatible=" + targetFingerprintsCompatible);
	}

	public static Presentation presentation(String layout) {
		try {
			ResourceDisplayEffectiveSnapshot snapshot = ResourceDisplayRuntime.snapshot();
			V7144ResourceDisplayResolver currentResolver;
			synchronized (V7144ResourceDisplayRightPanelBridge.class) { currentResolver = resolver; }
			Plan plan = ResourceDisplayRightPanelPlan.build(snapshot,
					groupId -> ResourceDisplayRuntime.currentHandlesForGroup(groupId));
			if (currentResolver == null || !targetFingerprintsCompatible || !plan.named()) {
				String reason = currentResolver == null ? "adapter bridge unavailable"
						: (!targetFingerprintsCompatible ? "target fingerprints incompatible" : plan.reason());
				Presentation fallback = Presentation.vanilla(reason);
				log(layout, fallback);
				return fallback;
			}

			List<Entry> entries = new ArrayList<Entry>(plan.entries().size());
			for (ResourceDisplayRightPanelPlan.Entry entry : plan.entries()) {
				if (entry.header()) {
					entries.add(Entry.header(entry.groupId(), entry.label(), entry.fallback()));
					continue;
				}
				RESOURCE resource = currentResolver.liveResource(entry.handle());
				if (resource.index() != entry.handle().observedIndex()
						|| resource.category != entry.handle().nativeCategory())
					throw new IllegalStateException("Live resource identity changed for " + entry.handle().resourceId());
				entries.add(Entry.resource(entry.groupId(), entry.fallback(), resource));
			}
			Presentation named = Presentation.named(entries, plan);
			log(layout, named);
			return named;
		} catch (Throwable failure) {
			ChoirDiagnostics.error("RESOURCE-DISPLAY right-panel failure layout=" + layout
					+ " reason=" + failure.getClass().getSimpleName() + ":" + failure.getMessage());
			Presentation fallback = Presentation.vanilla("bridge exception");
			log(layout, fallback);
			return fallback;
		}
	}

	private static void log(String layout, Presentation value) {
		ChoirDiagnostics.info("RESOURCE-DISPLAY right-panel mode="
				+ (value.named() ? "named-groups" : "vanilla-fallback")
				+ " layout=" + layout
				+ " runtime.generation=" + value.runtimeGeneration()
				+ " model.generation=" + value.modelGeneration()
				+ " model.signature=" + (value.modelSignature().isEmpty() ? "<none>" : value.modelSignature())
				+ " groups=" + value.groupCount()
				+ " explicit.groups=" + value.explicitGroupCount()
				+ " fallback.groups=" + value.fallbackGroupCount()
				+ " resources=" + value.resourceCount()
				+ " fallback.resources=" + value.fallbackResourceCount()
				+ " duplicate.rows=0 missing.rows=0"
				+ " reason=" + value.reason().replace(' ', '_'));
	}

	public static final class Entry {
		private final boolean header;
		private final String groupId;
		private final String label;
		private final boolean fallback;
		private final RESOURCE resource;

		private Entry(boolean header, String groupId, String label, boolean fallback, RESOURCE resource) {
			this.header = header;
			this.groupId = groupId;
			this.label = label;
			this.fallback = fallback;
			this.resource = resource;
		}
		static Entry header(String groupId, String label, boolean fallback) {
			return new Entry(true, groupId, label, fallback, null);
		}
		static Entry resource(String groupId, boolean fallback, RESOURCE resource) {
			return new Entry(false, groupId, "", fallback, resource);
		}
		public boolean header() { return header; }
		public String groupId() { return groupId; }
		public String label() { return label; }
		public boolean fallback() { return fallback; }
		public RESOURCE resource() { return resource; }
	}

	public static final class Presentation {
		private final boolean named;
		private final String reason;
		private final List<Entry> entries;
		private final long runtimeGeneration;
		private final long modelGeneration;
		private final String modelSignature;
		private final int groupCount;
		private final int fallbackGroupCount;
		private final int resourceCount;
		private final int fallbackResourceCount;

		private Presentation(boolean named, String reason, List<Entry> entries, long runtimeGeneration,
				long modelGeneration, String modelSignature, int groupCount, int fallbackGroupCount, int resourceCount,
				int fallbackResourceCount) {
			this.named = named;
			this.reason = reason;
			this.entries = List.copyOf(entries);
			this.runtimeGeneration = runtimeGeneration;
			this.modelGeneration = modelGeneration;
			this.modelSignature = modelSignature;
			this.groupCount = groupCount;
			this.fallbackGroupCount = fallbackGroupCount;
			this.resourceCount = resourceCount;
			this.fallbackResourceCount = fallbackResourceCount;
		}
		static Presentation vanilla(String reason) {
			return new Presentation(false, reason, List.of(), 0, 0, "", 0, 0, 0, 0);
		}
		static Presentation named(List<Entry> entries, Plan plan) {
			return new Presentation(true, plan.reason(), entries, plan.runtimeGeneration(), plan.modelGeneration(),
					plan.modelSignature(), plan.groupCount(), plan.fallbackGroupCount(), plan.resourceCount(), plan.fallbackResourceCount());
		}
		public boolean named() { return named; }
		public String reason() { return reason; }
		public List<Entry> entries() { return entries; }
		public long runtimeGeneration() { return runtimeGeneration; }
		public long modelGeneration() { return modelGeneration; }
		public String modelSignature() { return modelSignature; }
		public int groupCount() { return groupCount; }
		public int explicitGroupCount() { return groupCount - fallbackGroupCount; }
		public int fallbackGroupCount() { return fallbackGroupCount; }
		public int resourceCount() { return resourceCount; }
		public int fallbackResourceCount() { return fallbackResourceCount; }
	}
}
