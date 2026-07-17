package choir.internal.resources;

/** Stable metadata view of an adapter-private live resource handle. */
public interface ResourceDisplayRuntimeHandle {
	String resourceId();
	int canonicalPosition();
	int observedIndex();
	int nativeCategory();
	long runtimeRegistryGeneration();
	String registrySignature();
	int liveObjectIdentityToken();
}
