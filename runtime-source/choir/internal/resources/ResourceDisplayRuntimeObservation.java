package choir.internal.resources;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable adapter observation of one live registry. */
public final class ResourceDisplayRuntimeObservation {
	private final long runtimeRegistryGeneration;
	private final ResourceDisplayRegistryIdentity identity;
	private final String registrySignature;
	private final String orderedIdSignature;
	private final String orderedIndexSignature;
	private final String nativeCategorySignature;
	private final List<ResourceDisplayRuntimeHandle> handles;
	private final Map<String, ResourceDisplayRuntimeHandle> byId;
	private final List<String> adapterDiagnostics;

	public ResourceDisplayRuntimeObservation(long runtimeRegistryGeneration,
			ResourceDisplayRegistryIdentity identity, String registrySignature,
			String orderedIdSignature, String orderedIndexSignature, String nativeCategorySignature,
			List<? extends ResourceDisplayRuntimeHandle> handles) {
		this(runtimeRegistryGeneration, identity, registrySignature, orderedIdSignature, orderedIndexSignature,
				nativeCategorySignature, handles, List.of());
	}

	public ResourceDisplayRuntimeObservation(long runtimeRegistryGeneration,
			ResourceDisplayRegistryIdentity identity, String registrySignature,
			String orderedIdSignature, String orderedIndexSignature, String nativeCategorySignature,
			List<? extends ResourceDisplayRuntimeHandle> handles, List<String> adapterDiagnostics) {
		this.runtimeRegistryGeneration = runtimeRegistryGeneration;
		this.identity = identity;
		this.registrySignature = registrySignature;
		this.orderedIdSignature = orderedIdSignature;
		this.orderedIndexSignature = orderedIndexSignature;
		this.nativeCategorySignature = nativeCategorySignature;
		this.handles = List.copyOf(handles);
		this.adapterDiagnostics = List.copyOf(adapterDiagnostics);
		Map<String, ResourceDisplayRuntimeHandle> indexed = new LinkedHashMap<String, ResourceDisplayRuntimeHandle>();
		for (ResourceDisplayRuntimeHandle handle : this.handles) {
			if (handle.runtimeRegistryGeneration() != runtimeRegistryGeneration)
				throw new IllegalArgumentException("Runtime handle generation mismatch: " + handle.resourceId());
			if (!registrySignature.equals(handle.registrySignature()))
				throw new IllegalArgumentException("Runtime handle signature mismatch: " + handle.resourceId());
			if (indexed.put(handle.resourceId(), handle) != null)
				throw new IllegalArgumentException("Duplicate live resource ID: " + handle.resourceId());
		}
		this.byId = Map.copyOf(indexed);
	}

	public long runtimeRegistryGeneration() { return runtimeRegistryGeneration; }
	public ResourceDisplayRegistryIdentity identity() { return identity; }
	public String registrySignature() { return registrySignature; }
	public String orderedIdSignature() { return orderedIdSignature; }
	public String orderedIndexSignature() { return orderedIndexSignature; }
	public String nativeCategorySignature() { return nativeCategorySignature; }
	public List<ResourceDisplayRuntimeHandle> handles() { return handles; }
	public ResourceDisplayRuntimeHandle handle(String resourceId) { return byId.get(resourceId); }
	public List<String> adapterDiagnostics() { return adapterDiagnostics; }
	public int resourceCount() { return handles.size(); }
}
