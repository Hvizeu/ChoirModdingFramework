package choir.internal.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import choir.api.experimental.resources.ResourceDisplayEffectiveGroup;
import choir.api.experimental.resources.ResourceDisplayEffectiveSnapshot;
import choir.api.experimental.resources.ResourceDisplayRuntimeState;

/** Pure, game-type-free construction plan consumed by the V71.44 right-panel bridge. */
public final class ResourceDisplayRightPanelPlan {
	private ResourceDisplayRightPanelPlan() { }

	public static Plan build(ResourceDisplayEffectiveSnapshot snapshot, HandleProvider provider) {
		if (snapshot == null) return Plan.vanilla("effective snapshot unavailable");
		if (provider == null) return Plan.vanilla("live-handle provider unavailable");
		if (!snapshot.uiTargetsCompatible()) return Plan.vanilla("UI target fingerprints unavailable");
		if (snapshot.state() != ResourceDisplayRuntimeState.MODEL_READY)
			return Plan.vanilla("effective state=" + snapshot.state());
		if (snapshot.runtimeRegistryGeneration() <= 0 || snapshot.modelGeneration() <= 0
				|| snapshot.registrySignature().isBlank() || snapshot.modelSignature().isBlank())
			return Plan.vanilla("effective model generations or signatures unavailable");

		int explicitResources = 0;
		for (ResourceDisplayEffectiveGroup group : snapshot.groups())
			if (!group.fallback()) explicitResources += group.resourceIds().size();
		if (explicitResources == 0)
			return Plan.vanilla("no explicit display group affects the current model");

		try {
			List<Entry> entries = new ArrayList<Entry>();
			Set<String> seen = new HashSet<String>();
			int displayedGroups = 0;
			int displayedFallbackGroups = 0;
			int assignedResources = 0;
			int fallbackResources = 0;
			for (ResourceDisplayEffectiveGroup group : snapshot.groups()) {
				if (group.resourceIds().isEmpty()) continue;
				List<ResourceDisplayRuntimeHandle> handles = provider.handlesForGroup(group.groupId());
				if (handles == null || handles.size() != group.resourceIds().size())
					throw new IllegalStateException("handle count mismatch for " + group.groupId());
				entries.add(Entry.header(group.groupId(), group.label(), group.fallback()));
				displayedGroups++;
				if (group.fallback()) displayedFallbackGroups++;
				for (int i = 0; i < handles.size(); i++) {
					ResourceDisplayRuntimeHandle handle = handles.get(i);
					String expectedId = group.resourceIds().get(i);
					if (handle == null || !expectedId.equals(handle.resourceId()))
						throw new IllegalStateException("live handle order mismatch for " + expectedId);
					if (handle.runtimeRegistryGeneration() != snapshot.runtimeRegistryGeneration())
						throw new IllegalStateException("runtime generation mismatch for " + expectedId);
					if (!handle.registrySignature().equals(snapshot.registrySignature()))
						throw new IllegalStateException("registry signature mismatch for " + expectedId);
					if (handle.observedIndex() != handle.canonicalPosition())
						throw new IllegalStateException("canonical index mismatch for " + expectedId);
					if (!seen.add(expectedId)) throw new IllegalStateException("duplicate resource row " + expectedId);
					entries.add(Entry.resource(group.groupId(), group.fallback(), handle));
					if (group.fallback()) fallbackResources++; else assignedResources++;
				}
			}
			if (assignedResources != snapshot.assignedResourceCount())
				throw new IllegalStateException("assigned resource count mismatch");
			if (fallbackResources != snapshot.fallbackResourceCount())
				throw new IllegalStateException("fallback resource count mismatch");
			if (seen.size() != snapshot.canonicalResourceCount())
				throw new IllegalStateException("displayed resource count mismatch");
			return Plan.named(entries, snapshot.runtimeRegistryGeneration(), snapshot.modelGeneration(),
					snapshot.modelSignature(), displayedGroups, displayedFallbackGroups,
					assignedResources, fallbackResources);
		} catch (RuntimeException failure) {
			return Plan.vanilla("bridge validation failed: " + failure.getMessage());
		}
	}

	@FunctionalInterface
	public interface HandleProvider {
		List<ResourceDisplayRuntimeHandle> handlesForGroup(String groupId);
	}

	public static final class Entry {
		private final boolean header;
		private final String groupId;
		private final String label;
		private final boolean fallback;
		private final ResourceDisplayRuntimeHandle handle;

		private Entry(boolean header, String groupId, String label, boolean fallback,
				ResourceDisplayRuntimeHandle handle) {
			this.header = header;
			this.groupId = groupId;
			this.label = label;
			this.fallback = fallback;
			this.handle = handle;
		}

		static Entry header(String groupId, String label, boolean fallback) {
			return new Entry(true, groupId, label, fallback, null);
		}
		static Entry resource(String groupId, boolean fallback, ResourceDisplayRuntimeHandle handle) {
			return new Entry(false, groupId, "", fallback, handle);
		}

		public boolean header() { return header; }
		public String groupId() { return groupId; }
		public String label() { return label; }
		public boolean fallback() { return fallback; }
		public ResourceDisplayRuntimeHandle handle() { return handle; }
	}

	public static final class Plan {
		private final boolean named;
		private final String reason;
		private final List<Entry> entries;
		private final long runtimeGeneration;
		private final long modelGeneration;
		private final String modelSignature;
		private final int groupCount;
		private final int fallbackGroupCount;
		private final int assignedResourceCount;
		private final int fallbackResourceCount;

		private Plan(boolean named, String reason, List<Entry> entries, long runtimeGeneration,
				long modelGeneration, String modelSignature, int groupCount, int fallbackGroupCount,
				int assignedResourceCount, int fallbackResourceCount) {
			this.named = named;
			this.reason = reason;
			this.entries = List.copyOf(entries);
			this.runtimeGeneration = runtimeGeneration;
			this.modelGeneration = modelGeneration;
			this.modelSignature = modelSignature;
			this.groupCount = groupCount;
			this.fallbackGroupCount = fallbackGroupCount;
			this.assignedResourceCount = assignedResourceCount;
			this.fallbackResourceCount = fallbackResourceCount;
		}

		static Plan vanilla(String reason) {
			return new Plan(false, reason, List.of(), 0, 0, "", 0, 0, 0, 0);
		}
		static Plan named(List<Entry> entries, long runtimeGeneration, long modelGeneration,
				String modelSignature, int groupCount, int fallbackGroupCount,
				int assignedResourceCount, int fallbackResourceCount) {
			return new Plan(true, "named groups ready", entries, runtimeGeneration, modelGeneration,
					modelSignature, groupCount, fallbackGroupCount, assignedResourceCount, fallbackResourceCount);
		}

		public boolean named() { return named; }
		public String reason() { return reason; }
		public List<Entry> entries() { return entries; }
		public long runtimeGeneration() { return runtimeGeneration; }
		public long modelGeneration() { return modelGeneration; }
		public String modelSignature() { return modelSignature; }
		public int groupCount() { return groupCount; }
		public int fallbackGroupCount() { return fallbackGroupCount; }
		public int assignedResourceCount() { return assignedResourceCount; }
		public int fallbackResourceCount() { return fallbackResourceCount; }
		public int resourceCount() { return assignedResourceCount + fallbackResourceCount; }
	}
}
