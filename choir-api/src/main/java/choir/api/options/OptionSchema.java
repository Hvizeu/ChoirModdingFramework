package choir.api.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One provider-owned, automatically generated options page. */
public final class OptionSchema {
	private final String providerId;
	private final String displayName;
	private final String description;
	private final int schemaVersion;
	private final List<OptionSetting> settings;

	private OptionSchema(Builder builder) {
		providerId = builder.providerId;
		displayName = builder.displayName;
		description = builder.description;
		schemaVersion = builder.schemaVersion;
		settings = Collections.unmodifiableList(new ArrayList<OptionSetting>(builder.settings));
	}

	public static Builder builder(String providerId, String displayName) { return new Builder(providerId, displayName); }
	public String providerId() { return providerId; }
	public String displayName() { return displayName; }
	public String description() { return description; }
	public int schemaVersion() { return schemaVersion; }
	public List<OptionSetting> settings() { return settings; }

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof OptionSchema)) return false;
		OptionSchema other = (OptionSchema) object;
		return schemaVersion == other.schemaVersion && Objects.equals(providerId, other.providerId)
				&& Objects.equals(displayName, other.displayName) && Objects.equals(description, other.description)
				&& Objects.equals(settings, other.settings);
	}

	@Override
	public int hashCode() { return Objects.hash(providerId, displayName, description, Integer.valueOf(schemaVersion), settings); }

	public static final class Builder {
		private final String providerId;
		private final String displayName;
		private String description = "";
		private int schemaVersion = 1;
		private final List<OptionSetting> settings = new ArrayList<OptionSetting>();

		private Builder(String providerId, String displayName) {
			this.providerId = providerId;
			this.displayName = displayName == null ? providerId : displayName;
		}

		public Builder description(String value) { description = value == null ? "" : value; return this; }
		public Builder schemaVersion(int value) { schemaVersion = value; return this; }
		public Builder add(OptionSetting setting) { if (setting != null) settings.add(setting); return this; }
		public OptionSchema build() { return new OptionSchema(this); }
	}
}
