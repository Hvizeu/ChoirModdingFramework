package choir.api.experimental.resources;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Experimental and unstable presentation metadata. It never creates or mutates
 * a Songs of Syx resource and contains no numeric resource identity.
 */
public final class ResourceDisplayGroupDefinition {
	private final String providerId;
	private final String groupId;
	private final String localizationKey;
	private final String fallbackLabel;
	private final int definitionPriority;
	private final int sortOrder;
	private final Supplier<? extends CharSequence> labelResolver;

	private ResourceDisplayGroupDefinition(String providerId, String groupId, String localizationKey,
			String fallbackLabel, int definitionPriority, int sortOrder,
			Supplier<? extends CharSequence> labelResolver) {
		this.providerId = ResourceDisplayIds.provider(providerId);
		this.groupId = ResourceDisplayIds.group(groupId);
		this.localizationKey = ResourceDisplayIds.localization(localizationKey);
		this.fallbackLabel = ResourceDisplayIds.label(fallbackLabel);
		this.definitionPriority = definitionPriority;
		this.sortOrder = sortOrder;
		this.labelResolver = Objects.requireNonNull(labelResolver, "labelResolver");
	}

	public static ResourceDisplayGroupDefinition of(String providerId, String groupId,
			String localizationKey, String fallbackLabel) {
		return new ResourceDisplayGroupDefinition(providerId, groupId, localizationKey, fallbackLabel,
				0, 0, () -> fallbackLabel);
	}

	public ResourceDisplayGroupDefinition withDefinitionPriority(int value) {
		return new ResourceDisplayGroupDefinition(providerId, groupId, localizationKey, fallbackLabel,
				value, sortOrder, labelResolver);
	}

	public ResourceDisplayGroupDefinition withSortOrder(int value) {
		return new ResourceDisplayGroupDefinition(providerId, groupId, localizationKey, fallbackLabel,
				definitionPriority, value, labelResolver);
	}

	public ResourceDisplayGroupDefinition withLabelResolver(Supplier<? extends CharSequence> value) {
		return new ResourceDisplayGroupDefinition(providerId, groupId, localizationKey, fallbackLabel,
				definitionPriority, sortOrder, value);
	}

	public String providerId() { return providerId; }
	public String groupId() { return groupId; }
	public String localizationKey() { return localizationKey; }
	public String fallbackLabel() { return fallbackLabel; }
	public int definitionPriority() { return definitionPriority; }
	public int sortOrder() { return sortOrder; }
	public Supplier<? extends CharSequence> labelResolver() { return labelResolver; }
}
