package choir.internal.lifecycle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import choir.api.lifecycle.LifecycleEvent;
import choir.api.lifecycle.LifecycleListener;
import choir.api.lifecycle.LifecycleSubscriptionResult;
import choir.internal.ChoirDiagnostics;

public final class LifecycleEventBus {
	private static final Map<String, Subscription<?>> subscriptions = new TreeMap<String, Subscription<?>>();
	private static final Map<String, Object> latest = new HashMap<String, Object>();
	private LifecycleEventBus() { }
	public static <C> LifecycleSubscriptionResult subscribe(String providerId, String subscriptionId,
			LifecycleEvent<C> event, int priority, LifecycleListener<C> listener) {
		if (providerId == null || !providerId.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid lifecycle provider: " + providerId);
		if (subscriptionId == null || !subscriptionId.matches("[a-z][a-z0-9._-]*")) throw new IllegalArgumentException("Invalid subscription ID: " + subscriptionId);
		if (event == null || listener == null) throw new IllegalArgumentException("Event and listener are required.");
		String key = event.id() + '\u0000' + providerId + '\u0000' + subscriptionId;
		Object replay = null;
		synchronized (LifecycleEventBus.class) {
			Subscription<?> old = subscriptions.get(key);
			if (old != null) return old.priority == priority ? LifecycleSubscriptionResult.IDEMPOTENT : LifecycleSubscriptionResult.REJECTED_CONFLICT;
			subscriptions.put(key, new Subscription<C>(providerId, subscriptionId, event, priority, listener));
			if (event.replayLatest()) replay = latest.get(event.id());
		}
		ChoirDiagnostics.info("LIFECYCLE subscribe provider=" + providerId + " subscription=" + subscriptionId + " event=" + event.id() + " priority=" + priority + " replay=" + (replay != null));
		if (replay != null) invoke(listener, event.contextType().cast(replay), providerId, event.id(), true);
		return LifecycleSubscriptionResult.ACCEPTED;
	}
	public static <C> void publish(LifecycleEvent<C> event, C context) {
		if (event == null || context == null || !event.contextType().isInstance(context)) throw new IllegalArgumentException("Invalid lifecycle publication.");
		ArrayList<Subscription<C>> targets = new ArrayList<Subscription<C>>();
		synchronized (LifecycleEventBus.class) {
			if (event.replayLatest()) latest.put(event.id(), context);
			for (Subscription<?> candidate : subscriptions.values()) if (candidate.event.id().equals(event.id())) targets.add(cast(candidate, event.contextType()));
		}
		targets.sort(Comparator.comparingInt((Subscription<C> value) -> value.priority).reversed()
				.thenComparing(value -> value.providerId).thenComparing(value -> value.subscriptionId));
		for (Subscription<C> target : targets) invoke(target.listener, context, target.providerId, event.id(), false);
		ChoirDiagnostics.info("LIFECYCLE publish event=" + event.id() + " listeners=" + targets.size());
	}
	public static synchronized int subscriptionCount() { return subscriptions.size(); }
	static synchronized void resetForTests() { subscriptions.clear(); latest.clear(); }
	private static <C> void invoke(LifecycleListener<C> listener, C context, String providerId, String eventId, boolean replay) {
		try { listener.onEvent(context); }
		catch (Throwable failure) { ChoirDiagnostics.error("LIFECYCLE listener-failed provider=" + providerId + " event=" + eventId + " replay=" + replay + " cause=" + failure.getClass().getSimpleName()); }
	}
	@SuppressWarnings("unchecked") private static <C> Subscription<C> cast(Subscription<?> value, Class<C> type) {
		if (value.event.contextType() != type) throw new IllegalStateException("Lifecycle context type conflict.");
		return (Subscription<C>) value;
	}
	private static final class Subscription<C> {
		final String providerId, subscriptionId; final LifecycleEvent<C> event; final int priority; final LifecycleListener<C> listener;
		Subscription(String providerId, String subscriptionId, LifecycleEvent<C> event, int priority, LifecycleListener<C> listener) {
			this.providerId = providerId; this.subscriptionId = subscriptionId; this.event = event; this.priority = priority; this.listener = listener;
		}
	}
}
