package choir.internal.options;

import choir.api.options.OptionSchema;

/** Combined catalog/runtime view consumed by Choir's generated UI. */
public final class OptionsProviderView {
	private final String providerId;
	private final String displayName;
	private final String description;
	private final String modId;
	private final OptionSchema schema;

	OptionsProviderView(OptionsProviderCatalogEntry catalog, OptionSchema schema) {
		this.schema = schema;
		providerId = schema == null ? catalog.providerId() : schema.providerId();
		displayName = schema == null ? catalog.displayName() : schema.displayName();
		description = schema == null ? catalog.description() : schema.description();
		modId = catalog == null ? "" : catalog.modId();
	}

	public String providerId() { return providerId; }
	public String displayName() { return displayName; }
	public String description() { return description; }
	public String modId() { return modId; }
	public boolean runtimeRegistered() { return schema != null; }
	public OptionSchema schema() { return schema; }
}
