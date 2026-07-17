package choir.api.experimental.resources;

import java.util.List;
import java.util.Optional;

/** Immutable public view of the current effective presentation state. */
public final class ResourceDisplayEffectiveSnapshot {
	private final ResourceDisplayRuntimeState state;
	private final String detail;
	private final String adapterVersion;
	private final boolean uiTargetsCompatible;
	private final long registrationGeneration;
	private final long runtimeRegistryGeneration;
	private final long modelGeneration;
	private final long refreshGeneration;
	private final long localizationGeneration;
	private final String registrySignature;
	private final String modelSignature;
	private final int canonicalResourceCount;
	private final int assignedResourceCount;
	private final int fallbackResourceCount;
	private final List<ResourceDisplayEffectiveGroup> groups;

	public ResourceDisplayEffectiveSnapshot(ResourceDisplayRuntimeState state, String detail,
			String adapterVersion, boolean uiTargetsCompatible, long registrationGeneration,
			long runtimeRegistryGeneration, long modelGeneration, long refreshGeneration,
			long localizationGeneration, String registrySignature, String modelSignature,
			int canonicalResourceCount, int assignedResourceCount, int fallbackResourceCount,
			List<ResourceDisplayEffectiveGroup> groups) {
		this.state = state;
		this.detail = detail;
		this.adapterVersion = adapterVersion;
		this.uiTargetsCompatible = uiTargetsCompatible;
		this.registrationGeneration = registrationGeneration;
		this.runtimeRegistryGeneration = runtimeRegistryGeneration;
		this.modelGeneration = modelGeneration;
		this.refreshGeneration = refreshGeneration;
		this.localizationGeneration = localizationGeneration;
		this.registrySignature = registrySignature;
		this.modelSignature = modelSignature;
		this.canonicalResourceCount = canonicalResourceCount;
		this.assignedResourceCount = assignedResourceCount;
		this.fallbackResourceCount = fallbackResourceCount;
		this.groups = List.copyOf(groups);
	}

	public ResourceDisplayRuntimeState state() { return state; }
	public String detail() { return detail; }
	public String adapterVersion() { return adapterVersion; }
	public boolean uiTargetsCompatible() { return uiTargetsCompatible; }
	public long registrationGeneration() { return registrationGeneration; }
	public long runtimeRegistryGeneration() { return runtimeRegistryGeneration; }
	public long modelGeneration() { return modelGeneration; }
	public long refreshGeneration() { return refreshGeneration; }
	public long localizationGeneration() { return localizationGeneration; }
	public String registrySignature() { return registrySignature; }
	public String modelSignature() { return modelSignature; }
	public int canonicalResourceCount() { return canonicalResourceCount; }
	public int assignedResourceCount() { return assignedResourceCount; }
	public int fallbackResourceCount() { return fallbackResourceCount; }
	public List<ResourceDisplayEffectiveGroup> groups() { return groups; }

	public Optional<ResourceDisplayEffectiveGroup> group(String groupId) {
		for (ResourceDisplayEffectiveGroup group : groups) if (group.groupId().equals(groupId)) return Optional.of(group);
		return Optional.empty();
	}

	public Optional<ResourceDisplayEffectiveGroup> groupForResource(String resourceId) {
		for (ResourceDisplayEffectiveGroup group : groups)
			if (group.resourceIds().contains(resourceId)) return Optional.of(group);
		return Optional.empty();
	}
}
