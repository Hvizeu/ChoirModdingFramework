package choir.api.storage;

import java.util.Objects;

/**
 * Immutable descriptor that opts a complete room blueprint into advanced storage.
 *
 * <p>The declaration uses stable IDs only and is independent of the player's
 * global advanced-storage setting. It does not represent, extend, or expose a
 * vanilla Java room type.</p>
 */
public final class MultiResourceStorageDeclaration {
	private final String providerId;
	private final String declarationId;
	private final String roomKey;
	private final int maxResourceKinds;

	public MultiResourceStorageDeclaration(String providerId, String declarationId,
			String roomKey, int maxResourceKinds) {
		this.providerId = lowerId(providerId, "provider ID");
		this.declarationId = lowerId(declarationId, "declaration ID");
		this.roomKey = upperId(roomKey, "room key");
		if (maxResourceKinds < 1 || maxResourceKinds > ChoirStorage.HARD_MAX_RESOURCE_KINDS)
			throw new IllegalArgumentException("Maximum resource kinds must be between 1 and "
					+ ChoirStorage.HARD_MAX_RESOURCE_KINDS + ": " + maxResourceKinds);
		this.maxResourceKinds = maxResourceKinds;
	}

	public static MultiResourceStorageDeclaration forRoom(String providerId, String declarationId,
			String roomKey, int maxResourceKinds) {
		return new MultiResourceStorageDeclaration(providerId, declarationId, roomKey, maxResourceKinds);
	}

	public String providerId() { return providerId; }
	public String declarationId() { return declarationId; }
	public String roomKey() { return roomKey; }
	public int maxResourceKinds() { return maxResourceKinds; }
	public String qualifiedId() { return providerId + ":" + declarationId; }

	@Override public boolean equals(Object value) {
		if (this == value) return true;
		if (!(value instanceof MultiResourceStorageDeclaration)) return false;
		MultiResourceStorageDeclaration other = (MultiResourceStorageDeclaration) value;
		return maxResourceKinds == other.maxResourceKinds && providerId.equals(other.providerId)
				&& declarationId.equals(other.declarationId) && roomKey.equals(other.roomKey);
	}
	@Override public int hashCode() {
		return Objects.hash(providerId, declarationId, roomKey, Integer.valueOf(maxResourceKinds));
	}
	@Override public String toString() {
		return qualifiedId() + "[room=" + roomKey + ",maxResourceKinds=" + maxResourceKinds + "]";
	}

	private static String lowerId(String value, String label) {
		if (value == null || !value.matches("[a-z][a-z0-9._-]*"))
			throw new IllegalArgumentException(label + " must be a stable lowercase ID: " + value);
		return value;
	}
	private static String upperId(String value, String label) {
		if (value == null || !value.matches("[A-Z][A-Z0-9_]*"))
			throw new IllegalArgumentException(label + " must match [A-Z][A-Z0-9_]*: " + value);
		return value;
	}
}
