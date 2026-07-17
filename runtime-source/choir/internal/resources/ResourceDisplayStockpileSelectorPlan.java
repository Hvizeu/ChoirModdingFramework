package choir.internal.resources;

import java.util.ArrayList;
import java.util.List;

import choir.api.experimental.resources.ResourceDisplayEffectiveSnapshot;
import choir.internal.resources.ResourceDisplayRightPanelPlan.Entry;
import choir.internal.resources.ResourceDisplayRightPanelPlan.Plan;

/**
 * Game-type-free stockpile layout over the same validated presentation plan
 * consumed by the right panel. It owns no resource identity or selection state.
 */
public final class ResourceDisplayStockpileSelectorPlan {
	private ResourceDisplayStockpileSelectorPlan() { }

	public static SelectorPlan build(ResourceDisplayEffectiveSnapshot snapshot,
			ResourceDisplayRightPanelPlan.HandleProvider provider, int columns) {
		if (columns < 1) return SelectorPlan.vanilla("invalid column count");
		Plan shared = ResourceDisplayRightPanelPlan.build(snapshot, provider);
		if (!shared.named()) return SelectorPlan.vanilla(shared.reason());
		try {
			List<Group> groups = new ArrayList<Group>();
			String groupId = null;
			String label = null;
			boolean fallback = false;
			List<ResourceDisplayRuntimeHandle> handles = null;
			for (Entry entry : shared.entries()) {
				if (entry.header()) {
					if (handles != null) appendGroup(groups, groupId, label, fallback, handles, columns);
					groupId = entry.groupId();
					label = entry.label();
					fallback = entry.fallback();
					handles = new ArrayList<ResourceDisplayRuntimeHandle>();
				} else {
					if (handles == null || !entry.groupId().equals(groupId))
						throw new IllegalStateException("resource row without its group header");
					handles.add(entry.handle());
				}
			}
			if (handles != null) appendGroup(groups, groupId, label, fallback, handles, columns);
			int resources = groups.stream().mapToInt(group -> group.handles().size()).sum();
			if (resources != shared.resourceCount()) throw new IllegalStateException("selector resource count mismatch");
			if (groups.size() != shared.groupCount()) throw new IllegalStateException("selector group count mismatch");
			return SelectorPlan.named(groups, columns, shared);
		} catch (RuntimeException failure) {
			return SelectorPlan.vanilla("selector plan failed: " + failure.getMessage());
		}
	}

	private static void appendGroup(List<Group> groups, String groupId, String label, boolean fallback,
			List<ResourceDisplayRuntimeHandle> handles, int columns) {
		if (groupId == null || label == null || handles.isEmpty())
			throw new IllegalStateException("empty or invalid selector group");
		groups.add(new Group(groupId, label, fallback, handles, columns));
	}

	public static final class Group {
		private final String groupId;
		private final String label;
		private final boolean fallback;
		private final List<ResourceDisplayRuntimeHandle> handles;
		private final int resourceRows;

		private Group(String groupId, String label, boolean fallback,
				List<ResourceDisplayRuntimeHandle> handles, int columns) {
			this.groupId = groupId;
			this.label = label;
			this.fallback = fallback;
			this.handles = List.copyOf(handles);
			this.resourceRows = (handles.size() + columns - 1) / columns;
		}

		public String groupId() { return groupId; }
		public String label() { return label; }
		public boolean fallback() { return fallback; }
		public List<ResourceDisplayRuntimeHandle> handles() { return handles; }
		public int resourceRows() { return resourceRows; }
		public int totalRows() { return 2 + resourceRows; } // label + vanilla bulk controls + resources
	}

	public static final class SelectorPlan {
		private final boolean named;
		private final String reason;
		private final List<Group> groups;
		private final int columns;
		private final long runtimeGeneration;
		private final long modelGeneration;
		private final String modelSignature;
		private final int fallbackGroupCount;
		private final int assignedResourceCount;
		private final int fallbackResourceCount;

		private SelectorPlan(boolean named, String reason, List<Group> groups, int columns,
				long runtimeGeneration, long modelGeneration, String modelSignature,
				int fallbackGroupCount, int assignedResourceCount, int fallbackResourceCount) {
			this.named = named;
			this.reason = reason;
			this.groups = List.copyOf(groups);
			this.columns = columns;
			this.runtimeGeneration = runtimeGeneration;
			this.modelGeneration = modelGeneration;
			this.modelSignature = modelSignature;
			this.fallbackGroupCount = fallbackGroupCount;
			this.assignedResourceCount = assignedResourceCount;
			this.fallbackResourceCount = fallbackResourceCount;
		}

		static SelectorPlan vanilla(String reason) {
			return new SelectorPlan(false, reason, List.of(), 0, 0, 0, "", 0, 0, 0);
		}
		static SelectorPlan named(List<Group> groups, int columns, Plan shared) {
			return new SelectorPlan(true, shared.reason(), groups, columns, shared.runtimeGeneration(),
					shared.modelGeneration(), shared.modelSignature(), shared.fallbackGroupCount(),
					shared.assignedResourceCount(), shared.fallbackResourceCount());
		}

		public boolean named() { return named; }
		public String reason() { return reason; }
		public List<Group> groups() { return groups; }
		public int columns() { return columns; }
		public long runtimeGeneration() { return runtimeGeneration; }
		public long modelGeneration() { return modelGeneration; }
		public String modelSignature() { return modelSignature; }
		public int groupCount() { return groups.size(); }
		public int explicitGroupCount() { return groups.size() - fallbackGroupCount; }
		public int fallbackGroupCount() { return fallbackGroupCount; }
		public int assignedResourceCount() { return assignedResourceCount; }
		public int fallbackResourceCount() { return fallbackResourceCount; }
		public int resourceCount() { return assignedResourceCount + fallbackResourceCount; }
		public int headerRows() { return groups.size(); }
		public int bulkControlRows() { return groups.size(); }
		public int totalRows() { return groups.stream().mapToInt(Group::totalRows).sum(); }
	}
}
