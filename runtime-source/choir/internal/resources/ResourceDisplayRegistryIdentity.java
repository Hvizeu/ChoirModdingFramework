package choir.internal.resources;

/** Adapter-owned identity token with explicit same-registry semantics. */
public interface ResourceDisplayRegistryIdentity {
	boolean sameRegistry(ResourceDisplayRegistryIdentity other);
	String description();
}
