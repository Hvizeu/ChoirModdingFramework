package choir.api.race;

/** Directed race-to-race relationship patch; register both directions for a mutual relationship. */
public final class RaceRelationshipPatch {
	private final String providerId, patchId, raceId, otherRaceId;
	private final RaceNumericOperation operation;
	private final int priority;
	private final double value;
	private final RaceMissingTargetPolicy missingTargetPolicy;

	public RaceRelationshipPatch(String providerId, String patchId, String raceId, String otherRaceId,
			RaceNumericOperation operation, int priority, double value,
			RaceMissingTargetPolicy missingTargetPolicy) {
		this.providerId = RaceDeclaration.provider(providerId); this.patchId = RacePatchIds.patch(patchId);
		this.raceId = RaceDeclaration.race(raceId); this.otherRaceId = RaceDeclaration.race(otherRaceId);
		if (operation == null || missingTargetPolicy == null) throw new IllegalArgumentException("Relationship operation and missing-target policy are required.");
		if (!Double.isFinite(value)) throw new IllegalArgumentException("Relationship value must be finite.");
		if (operation == RaceNumericOperation.MULTIPLY && value < 0.0) throw new IllegalArgumentException("Relationship multiplier must not be negative.");
		this.operation = operation; this.priority = priority; this.value = value; this.missingTargetPolicy = missingTargetPolicy;
	}
	public String providerId() { return providerId; }
	public String patchId() { return patchId; }
	public String raceId() { return raceId; }
	public String otherRaceId() { return otherRaceId; }
	public RaceNumericOperation operation() { return operation; }
	public int priority() { return priority; }
	public double value() { return value; }
	public RaceMissingTargetPolicy missingTargetPolicy() { return missingTargetPolicy; }
}
