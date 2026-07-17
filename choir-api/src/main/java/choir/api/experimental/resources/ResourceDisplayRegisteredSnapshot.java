package choir.api.experimental.resources;

import java.util.List;

/** Immutable process-registration snapshot. */
public final class ResourceDisplayRegisteredSnapshot {
	private final long registrationGeneration;
	private final List<ResourceDisplayGroupDefinition> groups;
	private final List<ResourceDisplayAssignment> assignments;

	public ResourceDisplayRegisteredSnapshot(long registrationGeneration,
			List<ResourceDisplayGroupDefinition> groups, List<ResourceDisplayAssignment> assignments) {
		this.registrationGeneration = registrationGeneration;
		this.groups = List.copyOf(groups);
		this.assignments = List.copyOf(assignments);
	}

	public long registrationGeneration() { return registrationGeneration; }
	public List<ResourceDisplayGroupDefinition> groups() { return groups; }
	public List<ResourceDisplayAssignment> assignments() { return assignments; }
}
