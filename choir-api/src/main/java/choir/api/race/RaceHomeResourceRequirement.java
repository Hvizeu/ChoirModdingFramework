package choir.api.race;

/**
 * Additive requirement for one resource in a race/class household inventory.
 *
 * <p>The amount is a per-resident configurable maximum, not an immediate grant
 * or a construction cost. Choir preserves existing household-resource order and
 * composes several declarations by taking the greatest requested amount.</p>
 */
public final class RaceHomeResourceRequirement {
	private final String providerId;
	private final String requirementId;
	private final String raceId;
	private final RaceHomeResidentClass residentClass;
	private final String resourceId;
	private final int maxAmount;
	private final RaceMissingTargetPolicy missingTargetPolicy;

	public RaceHomeResourceRequirement(String providerId, String requirementId, String raceId,
			RaceHomeResidentClass residentClass, String resourceId, int maxAmount,
			RaceMissingTargetPolicy missingTargetPolicy) {
		this.providerId = RaceDeclaration.provider(providerId);
		this.requirementId = RacePatchIds.patch(requirementId);
		this.raceId = RaceDeclaration.race(raceId);
		if (residentClass == null) throw new IllegalArgumentException("A household resident class is required.");
		this.residentClass = residentClass;
		this.resourceId = RacePatchIds.subject(resourceId);
		if (maxAmount < 1 || maxAmount > 15)
			throw new IllegalArgumentException("Household resource maximum must be between 1 and 15.");
		this.maxAmount = maxAmount;
		if (missingTargetPolicy == null) throw new IllegalArgumentException("A missing-target policy is required.");
		this.missingTargetPolicy = missingTargetPolicy;
	}

	public String providerId() { return providerId; }
	public String requirementId() { return requirementId; }
	public String raceId() { return raceId; }
	public RaceHomeResidentClass residentClass() { return residentClass; }
	public String resourceId() { return resourceId; }
	public int maxAmount() { return maxAmount; }
	public RaceMissingTargetPolicy missingTargetPolicy() { return missingTargetPolicy; }
}
