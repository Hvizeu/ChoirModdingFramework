package choir.api.lifecycle;

@FunctionalInterface
public interface LifecycleListener<C> {
	void onEvent(C context);
}
