package choir.api.experimental.resources;

import java.util.List;

/** Stable-ID-only effective group returned to consumers. */
public final class ResourceDisplayEffectiveGroup {
	private final String groupId;
	private final String label;
	private final String localizationKey;
	private final String winningProviderId;
	private final List<String> contributorIds;
	private final List<String> resourceIds;
	private final boolean fallback;
	private final Integer nativeCategory;

	public ResourceDisplayEffectiveGroup(String groupId, String label, String localizationKey,
			String winningProviderId, List<String> contributorIds, List<String> resourceIds,
			boolean fallback, Integer nativeCategory) {
		this.groupId = groupId;
		this.label = label;
		this.localizationKey = localizationKey;
		this.winningProviderId = winningProviderId;
		this.contributorIds = List.copyOf(contributorIds);
		this.resourceIds = List.copyOf(resourceIds);
		this.fallback = fallback;
		this.nativeCategory = nativeCategory;
	}

	public String groupId() { return groupId; }
	public String label() { return label; }
	public String localizationKey() { return localizationKey; }
	public String winningProviderId() { return winningProviderId; }
	public List<String> contributorIds() { return contributorIds; }
	public List<String> resourceIds() { return resourceIds; }
	public boolean fallback() { return fallback; }
	public Integer nativeCategory() { return nativeCategory; }
}
