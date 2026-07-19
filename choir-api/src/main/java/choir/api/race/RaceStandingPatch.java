package choir.api.race;

/** Patches one user-facing race standing/like target for one humanoid class. */
public final class RaceStandingPatch {
	private final String providerId, patchId, raceId, statId, humanoidClassId;
	private final RaceNumericOperation operation;
	private final int priority;
	private final double value;
	private final RaceStandingPolarity polarity;
	private final RaceMissingTargetPolicy missingTargetPolicy;

	public RaceStandingPatch(String providerId, String patchId, String raceId, String statId,
			String humanoidClassId, RaceNumericOperation operation, int priority, double value,
			RaceStandingPolarity polarity, RaceMissingTargetPolicy missingTargetPolicy) {
		this.providerId = RaceDeclaration.provider(providerId); this.patchId = RacePatchIds.patch(patchId);
		this.raceId = RaceDeclaration.race(raceId); this.statId = RacePatchIds.subject(statId);
		this.humanoidClassId = RacePatchIds.subject(humanoidClassId);
		if (operation == null || polarity == null || missingTargetPolicy == null)
			throw new IllegalArgumentException("Standing operation, polarity, and missing-target policy are required.");
		if (!Double.isFinite(value) || value < 0.0 || value > 1_000_000.0)
			throw new IllegalArgumentException("Standing value must be finite and between 0 and 1000000.");
		this.operation = operation; this.priority = priority; this.value = value;
		this.polarity = polarity; this.missingTargetPolicy = missingTargetPolicy;
	}
	public String providerId() { return providerId; }
	public String patchId() { return patchId; }
	public String raceId() { return raceId; }
	public String statId() { return statId; }
	public String humanoidClassId() { return humanoidClassId; }
	public RaceNumericOperation operation() { return operation; }
	public int priority() { return priority; }
	public double value() { return value; }
	public RaceStandingPolarity polarity() { return polarity; }
	public RaceMissingTargetPolicy missingTargetPolicy() { return missingTargetPolicy; }
}
