package choir.api.patch;

@FunctionalInterface
public interface PatchComposer<T> {
	T compose(T current, T contribution);
}
