package choir.api.room;

import java.util.Objects;

/**
 * Immutable, data-backed room declaration. The provider owns the matching
 * Songs of Syx init, text, sprite and derived sound records.
 */
public final class RoomDeclaration {
	private final String providerId;
	private final String roomKey;
	private final RoomFamily family;
	private final String spriteKey;
	private final RoomMissingProviderPolicy missingProviderPolicy;

	private RoomDeclaration(String providerId, String roomKey, RoomFamily family, String spriteKey) {
		this.providerId = providerId(providerId);
		this.roomKey = upperKey(roomKey, "room key");
		this.family = Objects.requireNonNull(family, "family");
		this.spriteKey = upperKey(spriteKey, "sprite key");
		this.missingProviderPolicy = RoomMissingProviderPolicy.REQUIRE_PROVIDER_FOR_SAVE_LOAD;
	}

	/**
	 * Declares a passive decoration with no employment, service, industry,
	 * production, storage, scripted update or Choir-owned save payload.
	 */
	public static RoomDeclaration passiveDecoration(String providerId, String roomKey, String spriteKey) {
		return new RoomDeclaration(providerId, roomKey, RoomFamily.PASSIVE_DECORATION, spriteKey);
	}

	public String providerId() { return providerId; }
	public String roomKey() { return roomKey; }
	public RoomFamily family() { return family; }
	public String spriteKey() { return spriteKey; }
	public RoomMissingProviderPolicy missingProviderPolicy() { return missingProviderPolicy; }
	public String qualifiedId() { return providerId + ":" + roomKey; }

	@Override public boolean equals(Object value) {
		if (this == value) return true;
		if (!(value instanceof RoomDeclaration)) return false;
		RoomDeclaration other = (RoomDeclaration) value;
		return providerId.equals(other.providerId) && roomKey.equals(other.roomKey)
				&& family == other.family && spriteKey.equals(other.spriteKey)
				&& missingProviderPolicy == other.missingProviderPolicy;
	}

	@Override public int hashCode() { return Objects.hash(providerId, roomKey, family, spriteKey, missingProviderPolicy); }
	@Override public String toString() { return qualifiedId() + "[" + family + ",sprite=" + spriteKey
			+ ",missingProvider=" + missingProviderPolicy + "]"; }

	private static String providerId(String value) {
		if (value == null || !value.matches("[a-z][a-z0-9._-]*"))
			throw new IllegalArgumentException("Provider ID must be a stable lowercase ID: " + value);
		return value;
	}

	private static String upperKey(String value, String label) {
		if (value == null || !value.matches("[A-Z][A-Z0-9_]*"))
			throw new IllegalArgumentException(label + " must match [A-Z][A-Z0-9_]*: " + value);
		return value;
	}
}
