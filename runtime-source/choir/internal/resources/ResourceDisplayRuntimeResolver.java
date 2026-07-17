package choir.internal.resources;

/** Version adapter boundary. Implementations own all live game references. */
public interface ResourceDisplayRuntimeResolver {
	ResourceDisplayRuntimeObservation observe(long proposedRuntimeRegistryGeneration);
	void verifyUnchanged(ResourceDisplayRuntimeObservation observation);
}
