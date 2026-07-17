package choir.internal;

import choir.api.Capability;
import choir.api.RoomRegistrationProbe;
import choir.api.experimental.resources.ResourceDisplayAssignment;
import choir.api.experimental.resources.ResourceDisplayEffectiveSnapshot;
import choir.api.experimental.resources.ResourceDisplayGroupDefinition;
import choir.api.experimental.resources.ResourceDisplayRefreshResult;
import choir.api.experimental.resources.ResourceDisplayRegisteredSnapshot;
import choir.api.experimental.resources.ResourceDisplayRegistrationResult;
import choir.api.lifecycle.LifecycleEvent;
import choir.api.lifecycle.LifecycleListener;
import choir.api.lifecycle.LifecycleSubscriptionResult;
import choir.api.options.OptionChangeListener;
import choir.api.options.OptionListenerRegistrationResult;
import choir.api.options.OptionRegistrationResult;
import choir.api.options.OptionSchema;
import choir.api.patch.PatchContribution;
import choir.api.patch.PatchRegistrationResult;
import choir.api.patch.PatchResolution;
import choir.api.patch.PatchTarget;
import choir.api.platform.PlatformSnapshot;
import choir.api.race.RaceBoostPatch;
import choir.api.race.RaceDeclaration;
import choir.api.race.RacePreferencePatch;
import choir.api.race.RaceRegistrationResult;
import choir.api.race.RaceRuntimeSnapshot;
import choir.api.room.RoomDeclaration;
import choir.api.room.RoomRegistrationResult;
import choir.api.room.RoomRegistrationSnapshot;
import choir.api.spi.ChoirRuntimeService;
import choir.internal.lifecycle.LifecycleEventBus;
import choir.internal.options.OptionsRegistry;
import choir.internal.patch.PatchRegistry;
import choir.internal.platform.PlatformRuntime;
import choir.internal.race.RaceRegistry;
import choir.internal.resources.ResourceDisplayRegistry;
import choir.internal.resources.ResourceDisplayRuntime;
import choir.internal.room.RoomRegistry;

/** Runtime-side delegate. It is the only implementation of the public API service contract. */
public final class ApiRuntimeService implements ChoirRuntimeService {
	public static final ApiRuntimeService INSTANCE = new ApiRuntimeService();
	private ApiRuntimeService() { }

	@Override public boolean hasCapability(Capability capability) { return ChoirBootstrap.hasCapability(capability); }
	@Override public boolean registerRoomRegistrationProbe(RoomRegistrationProbe probe) { return RoomRegistrationRegistry.register(probe); }
	@Override public RoomRegistrationResult registerRoom(RoomDeclaration declaration) { return RoomRegistry.register(declaration); }
	@Override public RoomRegistrationSnapshot roomSnapshot() { return RoomRegistry.snapshot(); }
	@Override public RaceRegistrationResult declareDataBackedRace(RaceDeclaration declaration) { return RaceRegistry.declare(declaration); }
	@Override public RaceRegistrationResult patchRaceBoost(RaceBoostPatch patch) { return RaceRegistry.patch(patch); }
	@Override public RaceRegistrationResult patchRacePreference(RacePreferencePatch patch) { return RaceRegistry.patchPreference(patch); }
	@Override public RaceRuntimeSnapshot raceRuntimeSnapshot() { return RaceRegistry.runtimeSnapshot(); }
	@Override public <C> LifecycleSubscriptionResult subscribeLifecycle(String providerId, String subscriptionId,
			LifecycleEvent<C> event, int priority, LifecycleListener<C> listener) {
		return LifecycleEventBus.subscribe(providerId, subscriptionId, event, priority, listener);
	}
	@Override public PlatformSnapshot platformSnapshot() { return PlatformRuntime.snapshot(); }
	@Override public String validationSessionId() { return ChoirDiagnostics.validationSessionId(); }
	@Override public <T> PatchRegistrationResult registerPatchTarget(PatchTarget<T> target) { return PatchRegistry.registerTarget(target); }
	@Override public <T> PatchRegistrationResult contributePatch(PatchContribution<T> contribution) { return PatchRegistry.contribute(contribution); }
	@Override public <T> PatchResolution<T> resolvePatch(String targetId, Class<T> valueType) { return PatchRegistry.resolve(targetId, valueType); }
	@Override public OptionRegistrationResult registerOptions(OptionSchema schema) { return OptionsRegistry.register(schema); }
	@Override public OptionListenerRegistrationResult subscribeOptions(String providerId, OptionChangeListener listener) {
		return OptionsRegistry.subscribe(providerId, listener);
	}
	@Override public boolean getBooleanOption(String providerId, String key, boolean fallback) { return OptionsRegistry.getBoolean(providerId, key, fallback); }
	@Override public int getIntOption(String providerId, String key, int fallback) { return OptionsRegistry.getInt(providerId, key, fallback); }
	@Override public double getDoubleOption(String providerId, String key, double fallback) { return OptionsRegistry.getDouble(providerId, key, fallback); }
	@Override public String getStringOption(String providerId, String key, String fallback) { return OptionsRegistry.getString(providerId, key, fallback); }
	@Override public boolean resetOptionsToDefaults(String providerId) { return OptionsRegistry.resetCurrentToDefaults(providerId); }
	@Override public ResourceDisplayRegistrationResult registerResourceDisplayGroup(ResourceDisplayGroupDefinition definition) {
		return ResourceDisplayRegistry.registerGroup(definition);
	}
	@Override public ResourceDisplayRegistrationResult registerResourceDisplayAssignment(ResourceDisplayAssignment assignment) {
		return ResourceDisplayRegistry.registerAssignment(assignment);
	}
	@Override public long resourceDisplayRegistrationGeneration() { return ResourceDisplayRegistry.generation(); }
	@Override public ResourceDisplayRegisteredSnapshot resourceDisplayRegisteredSnapshot() { return ResourceDisplayRegistry.publicSnapshot(); }
	@Override public ResourceDisplayEffectiveSnapshot resourceDisplayEffectiveSnapshot() { return ResourceDisplayRuntime.snapshot(); }
	@Override public ResourceDisplayRefreshResult requestResourceDisplayRefresh() { return ResourceDisplayRuntime.requestRefresh(); }
	@Override public void setResourceDisplayEnabled(boolean enabled) { ResourceDisplayRuntime.setEnabled(enabled); }
	@Override public boolean isResourceDisplayEnabled() { return ResourceDisplayRuntime.isEnabled(); }
}
