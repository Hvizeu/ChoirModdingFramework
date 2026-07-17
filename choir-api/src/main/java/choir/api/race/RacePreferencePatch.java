package choir.api.race;

import java.util.List;
import java.util.TreeSet;

/**
 * Game-type-free patch for a race's food or drink preference collection.
 * Resource identifiers are public Choir stable IDs, not live RESOURCE objects.
 */
public final class RacePreferencePatch {
	private final String providerId;
	private final String patchId;
	private final String raceId;
	private final RacePreferenceKind kind;
	private final RacePreferenceOperation operation;
	private final int priority;
	private final List<String> resourceIds;

	public RacePreferencePatch(String providerId, String patchId, String raceId,
			RacePreferenceKind kind, RacePreferenceOperation operation, int priority,
			List<String> resourceIds) {
		this.providerId = RaceDeclaration.provider(providerId);
		if (patchId == null || !patchId.matches("[a-z][a-z0-9._-]*"))
			throw new IllegalArgumentException("Invalid patch ID: " + patchId);
		this.patchId = patchId;
		this.raceId = RaceDeclaration.race(raceId);
		if (kind == null || operation == null) throw new IllegalArgumentException("Preference kind and operation are required.");
		if (resourceIds == null || resourceIds.isEmpty()) throw new IllegalArgumentException("At least one resource ID is required.");
		TreeSet<String> sorted = new TreeSet<String>();
		for (String resourceId : resourceIds) sorted.add(resource(resourceId));
		this.kind = kind;
		this.operation = operation;
		this.priority = priority;
		this.resourceIds = List.copyOf(sorted);
	}

	public String providerId() { return providerId; }
	public String patchId() { return patchId; }
	public String raceId() { return raceId; }
	public RacePreferenceKind kind() { return kind; }
	public RacePreferenceOperation operation() { return operation; }
	public int priority() { return priority; }
	public List<String> resourceIds() { return resourceIds; }

	private static String resource(String id) {
		if (id == null || !id.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("Invalid resource ID: " + id);
		return id;
	}
}
