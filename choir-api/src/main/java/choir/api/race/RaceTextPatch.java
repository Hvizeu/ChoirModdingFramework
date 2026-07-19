package choir.api.race;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Composable prose or ordered phrase patch for one race. */
public final class RaceTextPatch {
	private final String providerId, patchId, raceId;
	private final RaceTextField field;
	private final RaceCollectionOperation operation;
	private final int priority;
	private final List<String> values;
	private final RaceMissingTargetPolicy missingRacePolicy;

	public RaceTextPatch(String providerId, String patchId, String raceId, RaceTextField field,
			RaceCollectionOperation operation, int priority, List<String> values,
			RaceMissingTargetPolicy missingRacePolicy) {
		this.providerId = RaceDeclaration.provider(providerId);
		this.patchId = RacePatchIds.patch(patchId);
		this.raceId = RaceDeclaration.race(raceId);
		if (field == null || operation == null || missingRacePolicy == null)
			throw new IllegalArgumentException("Text field, operation, and missing-race policy are required.");
		if (values == null || values.isEmpty()) throw new IllegalArgumentException("At least one text value is required.");
		LinkedHashSet<String> unique = new LinkedHashSet<String>();
		for (String value : values) {
			if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Race text values must not be blank.");
			unique.add(value.trim());
		}
		if (!field.isCollection() && operation == RaceCollectionOperation.REMOVE)
			throw new IllegalArgumentException("Scalar race prose does not support REMOVE; use REPLACE with the intended value.");
		this.field = field; this.operation = operation; this.priority = priority;
		this.values = List.copyOf(new ArrayList<String>(unique)); this.missingRacePolicy = missingRacePolicy;
	}
	public String providerId() { return providerId; }
	public String patchId() { return patchId; }
	public String raceId() { return raceId; }
	public RaceTextField field() { return field; }
	public RaceCollectionOperation operation() { return operation; }
	public int priority() { return priority; }
	public List<String> values() { return values; }
	public RaceMissingTargetPolicy missingRacePolicy() { return missingRacePolicy; }
}
