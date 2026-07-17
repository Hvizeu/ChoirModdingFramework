package choir.api.race;

/** Declares ownership of a normal-data-backed race; Choir does not create race content. */
public final class RaceDeclaration {
	private final String providerId;
	private final String raceId;
	private final String displayName;
	public RaceDeclaration(String providerId, String raceId, String displayName) {
		this.providerId = provider(providerId); this.raceId = race(raceId);
		if (displayName == null || displayName.trim().isEmpty()) throw new IllegalArgumentException("Race display name is required.");
		this.displayName = displayName.trim();
	}
	public String providerId() { return providerId; }
	public String raceId() { return raceId; }
	public String displayName() { return displayName; }
	static String provider(String id) { if (id == null || !id.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid provider ID: " + id); return id; }
	static String race(String id) { if (id == null || !id.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("Invalid race ID: " + id); return id; }
}
