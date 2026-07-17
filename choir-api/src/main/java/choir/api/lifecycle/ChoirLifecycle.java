package choir.api.lifecycle;

import choir.api.spi.ChoirRuntimeServices;

public final class ChoirLifecycle {
	public static final int API_VERSION = 1;
	private ChoirLifecycle() { }
	public static <C> LifecycleSubscriptionResult subscribe(String providerId, String subscriptionId,
			LifecycleEvent<C> event, int priority, LifecycleListener<C> listener) {
		return ChoirRuntimeServices.require().subscribeLifecycle(providerId, subscriptionId, event, priority, listener);
	}
}
