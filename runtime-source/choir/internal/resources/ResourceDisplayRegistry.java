package choir.internal.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import choir.api.experimental.resources.ResourceDisplayAssignment;
import choir.api.experimental.resources.ResourceDisplayGroupDefinition;
import choir.api.experimental.resources.ResourceDisplayRegistrationResult;
import choir.api.experimental.resources.ResourceDisplayRegisteredSnapshot;
import choir.internal.ChoirDiagnostics;

/** Process-scope retention for presentation descriptors; no game objects live here. */
public final class ResourceDisplayRegistry {
	private static final State PROCESS = new State();

	private ResourceDisplayRegistry() { }

	public static ResourceDisplayRegistrationResult registerGroup(ResourceDisplayGroupDefinition definition) {
		if (definition == null) throw new IllegalArgumentException("Resource display group definition must not be null.");
		ResourceDisplayRegistrationResult result = PROCESS.registerGroup(definition);
		if (result == ResourceDisplayRegistrationResult.ACCEPTED) ResourceDisplayRuntime.registrationChanged();
		ChoirDiagnostics.beginValidationSession();
		ChoirDiagnostics.info("RESOURCE-DISPLAY group-register provider=" + definition.providerId()
				+ " group=" + definition.groupId() + " result=" + result);
		return result;
	}

	public static ResourceDisplayRegistrationResult registerAssignment(ResourceDisplayAssignment assignment) {
		if (assignment == null) throw new IllegalArgumentException("Resource display assignment must not be null.");
		ResourceDisplayRegistrationResult result = PROCESS.registerAssignment(assignment);
		if (result == ResourceDisplayRegistrationResult.ACCEPTED) ResourceDisplayRuntime.registrationChanged();
		ChoirDiagnostics.beginValidationSession();
		ChoirDiagnostics.info("RESOURCE-DISPLAY assignment-register provider=" + assignment.providerId()
				+ " resource=" + assignment.resourceId() + " group=" + assignment.groupId()
				+ " result=" + result);
		return result;
	}

	public static long generation() { return PROCESS.generation(); }
	public static Snapshot snapshot() { return PROCESS.snapshot(); }
	public static ResourceDisplayRegisteredSnapshot publicSnapshot() {
		Snapshot snapshot = PROCESS.snapshot();
		return new ResourceDisplayRegisteredSnapshot(snapshot.registrationGeneration(), snapshot.groups(), snapshot.assignments());
	}

	static final class State {
		private final Map<String, ResourceDisplayGroupDefinition> groups = new TreeMap<String, ResourceDisplayGroupDefinition>();
		private final Map<String, ResourceDisplayAssignment> assignments = new TreeMap<String, ResourceDisplayAssignment>();
		private long generation;

		synchronized ResourceDisplayRegistrationResult registerGroup(ResourceDisplayGroupDefinition value) {
			if (value == null) throw new IllegalArgumentException("Resource display group definition must not be null.");
			String key = value.providerId() + '\u0000' + value.groupId();
			ResourceDisplayGroupDefinition old = groups.get(key);
			if (old == null) {
				groups.put(key, value);
				generation++;
				return ResourceDisplayRegistrationResult.ACCEPTED;
			}
			return equivalent(old, value)
					? ResourceDisplayRegistrationResult.IDEMPOTENT
					: ResourceDisplayRegistrationResult.REJECTED_CONFLICT;
		}

		synchronized ResourceDisplayRegistrationResult registerAssignment(ResourceDisplayAssignment value) {
			if (value == null) throw new IllegalArgumentException("Resource display assignment must not be null.");
			String key = value.providerId() + '\u0000' + value.resourceId() + '\u0000' + value.groupId();
			ResourceDisplayAssignment old = assignments.get(key);
			if (old == null) {
				assignments.put(key, value);
				generation++;
				return ResourceDisplayRegistrationResult.ACCEPTED;
			}
			return equivalent(old, value)
					? ResourceDisplayRegistrationResult.IDEMPOTENT
					: ResourceDisplayRegistrationResult.REJECTED_CONFLICT;
		}

		synchronized long generation() { return generation; }

		synchronized Snapshot snapshot() {
			return new Snapshot(generation, new ArrayList<ResourceDisplayGroupDefinition>(groups.values()),
					new ArrayList<ResourceDisplayAssignment>(assignments.values()));
		}
	}

	public static final class Snapshot {
		private final long registrationGeneration;
		private final List<ResourceDisplayGroupDefinition> groups;
		private final List<ResourceDisplayAssignment> assignments;

		private Snapshot(long registrationGeneration, List<ResourceDisplayGroupDefinition> groups,
				List<ResourceDisplayAssignment> assignments) {
			this.registrationGeneration = registrationGeneration;
			this.groups = List.copyOf(groups);
			this.assignments = List.copyOf(assignments);
		}

		public long registrationGeneration() { return registrationGeneration; }
		public List<ResourceDisplayGroupDefinition> groups() { return groups; }
		public List<ResourceDisplayAssignment> assignments() { return assignments; }
	}

	private static boolean equivalent(ResourceDisplayGroupDefinition a, ResourceDisplayGroupDefinition b) {
		return a.providerId().equals(b.providerId()) && a.groupId().equals(b.groupId())
				&& a.localizationKey().equals(b.localizationKey()) && a.fallbackLabel().equals(b.fallbackLabel())
				&& a.definitionPriority() == b.definitionPriority() && a.sortOrder() == b.sortOrder();
	}

	private static boolean equivalent(ResourceDisplayAssignment a, ResourceDisplayAssignment b) {
		return a.providerId().equals(b.providerId()) && a.resourceId().equals(b.resourceId())
				&& a.groupId().equals(b.groupId()) && a.assignmentPriority() == b.assignmentPriority()
				&& a.resourceSortOrder() == b.resourceSortOrder();
	}
}
