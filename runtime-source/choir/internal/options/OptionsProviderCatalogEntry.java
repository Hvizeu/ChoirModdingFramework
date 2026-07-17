package choir.internal.options;

import java.util.Objects;

/** Immutable declaration read from one enabled mod's static Choir metadata. */
public final class OptionsProviderCatalogEntry {
	private final String modId;
	private final String providerId;
	private final String displayName;
	private final String description;

	public OptionsProviderCatalogEntry(String modId, String providerId, String displayName, String description) {
		this.modId = modId == null ? "" : modId;
		this.providerId = providerId == null ? "" : providerId;
		this.displayName = displayName == null ? "" : displayName;
		this.description = description == null ? "" : description;
	}

	public String modId() { return modId; }
	public String providerId() { return providerId; }
	public String displayName() { return displayName; }
	public String description() { return description; }

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof OptionsProviderCatalogEntry)) return false;
		OptionsProviderCatalogEntry other = (OptionsProviderCatalogEntry) object;
		return Objects.equals(modId, other.modId) && Objects.equals(providerId, other.providerId)
				&& Objects.equals(displayName, other.displayName) && Objects.equals(description, other.description);
	}

	@Override
	public int hashCode() { return Objects.hash(modId, providerId, displayName, description); }
}
