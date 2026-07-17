package choir.api.experimental.resources;

/** Immutable assignment by stable resource and group IDs only. */
public final class ResourceDisplayAssignment {
	private final String providerId;
	private final String resourceId;
	private final String groupId;
	private final int assignmentPriority;
	private final int resourceSortOrder;

	private ResourceDisplayAssignment(String providerId, String resourceId, String groupId,
			int assignmentPriority, int resourceSortOrder) {
		this.providerId = ResourceDisplayIds.provider(providerId);
		this.resourceId = ResourceDisplayIds.resource(resourceId);
		this.groupId = ResourceDisplayIds.group(groupId);
		this.assignmentPriority = assignmentPriority;
		this.resourceSortOrder = resourceSortOrder;
	}

	public static ResourceDisplayAssignment of(String providerId, String resourceId, String groupId) {
		return new ResourceDisplayAssignment(providerId, resourceId, groupId, 0, 0);
	}

	public ResourceDisplayAssignment withAssignmentPriority(int value) {
		return new ResourceDisplayAssignment(providerId, resourceId, groupId, value, resourceSortOrder);
	}

	public ResourceDisplayAssignment withResourceSortOrder(int value) {
		return new ResourceDisplayAssignment(providerId, resourceId, groupId, assignmentPriority, value);
	}

	public String providerId() { return providerId; }
	public String resourceId() { return resourceId; }
	public String groupId() { return groupId; }
	public int assignmentPriority() { return assignmentPriority; }
	public int resourceSortOrder() { return resourceSortOrder; }
}
