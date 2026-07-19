package choir.api.race;

/** Composable patch for one stable numeric race attribute target. */
public final class RaceNumericPatch {
	private final String providerId, patchId, raceId, subjectId;
	private final RaceNumericAttribute attribute;
	private final RaceNumericOperation operation;
	private final int priority;
	private final double value;
	private final RaceMissingTargetPolicy missingTargetPolicy;

	public RaceNumericPatch(String providerId, String patchId, String raceId,
			RaceNumericAttribute attribute, String subjectId, RaceNumericOperation operation,
			int priority, double value, RaceMissingTargetPolicy missingTargetPolicy) {
		this.providerId = RaceDeclaration.provider(providerId); this.patchId = RacePatchIds.patch(patchId);
		this.raceId = RaceDeclaration.race(raceId);
		if (attribute == null || operation == null || missingTargetPolicy == null)
			throw new IllegalArgumentException("Numeric attribute, operation, and missing-target policy are required.");
		if (!Double.isFinite(value)) throw new IllegalArgumentException("Numeric race patch value must be finite.");
		if (operation == RaceNumericOperation.MULTIPLY && value < 0.0)
			throw new IllegalArgumentException("Numeric race multipliers must not be negative.");
		if (attribute.subjectRequired()) this.subjectId = RacePatchIds.subject(subjectId);
		else {
			if (subjectId != null && !subjectId.trim().isEmpty()) throw new IllegalArgumentException(attribute + " does not accept a subject ID.");
			this.subjectId = "";
		}
		this.attribute = attribute; this.operation = operation; this.priority = priority;
		this.value = value; this.missingTargetPolicy = missingTargetPolicy;
	}
	public String providerId() { return providerId; }
	public String patchId() { return patchId; }
	public String raceId() { return raceId; }
	public RaceNumericAttribute attribute() { return attribute; }
	public String subjectId() { return subjectId; }
	public RaceNumericOperation operation() { return operation; }
	public int priority() { return priority; }
	public double value() { return value; }
	public RaceMissingTargetPolicy missingTargetPolicy() { return missingTargetPolicy; }
}
