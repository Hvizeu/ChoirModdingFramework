package choir.api.lifecycle;

public final class LifecycleEvents {
	private LifecycleEvents() { }
	public static final LifecycleEvent<LifecycleContext> BEFORE_GAME_CREATED = event("choir.lifecycle.before-game-created");
	public static final LifecycleEvent<LifecycleContext> GAME_INITIALIZED = event("choir.lifecycle.game-initialized");
	public static final LifecycleEvent<LifecycleContext> INSTANCE_CREATED = event("choir.lifecycle.instance-created");
	public static final LifecycleEvent<LifecycleContext> GAMEPLAY_REACHED = event("choir.lifecycle.gameplay-reached");
	public static final LifecycleEvent<LifecycleContext> GAME_DISPOSING = event("choir.lifecycle.game-disposing");
	private static LifecycleEvent<LifecycleContext> event(String id) { return new LifecycleEvent<LifecycleContext>(id, LifecycleContext.class, true); }
}
