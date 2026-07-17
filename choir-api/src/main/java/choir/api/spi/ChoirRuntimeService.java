package choir.api.spi;

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

/**
 * Runtime-owned implementation contract for Choir's public facades.
 *
 * <p>This type exists so {@code choir-api} can be compiled, documented, and
 * consumed without importing Choir internals or V71.44 game classes. It is
 * installed only by the matching Choir runtime JAR. Consumer mods must use
 * the normal {@code choir.api} facades and must not implement or install this
 * service.</p>
 */
public interface ChoirRuntimeService {

	boolean hasCapability(Capability capability);
	boolean registerRoomRegistrationProbe(RoomRegistrationProbe probe);

	RoomRegistrationResult registerRoom(RoomDeclaration declaration);
	RoomRegistrationSnapshot roomSnapshot();

	RaceRegistrationResult declareDataBackedRace(RaceDeclaration declaration);
	RaceRegistrationResult patchRaceBoost(RaceBoostPatch patch);
	RaceRegistrationResult patchRacePreference(RacePreferencePatch patch);
	RaceRuntimeSnapshot raceRuntimeSnapshot();

	<C> LifecycleSubscriptionResult subscribeLifecycle(String providerId, String subscriptionId,
			LifecycleEvent<C> event, int priority, LifecycleListener<C> listener);

	PlatformSnapshot platformSnapshot();
	String validationSessionId();

	<T> PatchRegistrationResult registerPatchTarget(PatchTarget<T> target);
	<T> PatchRegistrationResult contributePatch(PatchContribution<T> contribution);
	<T> PatchResolution<T> resolvePatch(String targetId, Class<T> valueType);

	OptionRegistrationResult registerOptions(OptionSchema schema);
	OptionListenerRegistrationResult subscribeOptions(String providerId, OptionChangeListener listener);
	boolean getBooleanOption(String providerId, String key, boolean fallback);
	int getIntOption(String providerId, String key, int fallback);
	double getDoubleOption(String providerId, String key, double fallback);
	String getStringOption(String providerId, String key, String fallback);
	boolean resetOptionsToDefaults(String providerId);

	ResourceDisplayRegistrationResult registerResourceDisplayGroup(ResourceDisplayGroupDefinition definition);
	ResourceDisplayRegistrationResult registerResourceDisplayAssignment(ResourceDisplayAssignment assignment);
	long resourceDisplayRegistrationGeneration();
	ResourceDisplayRegisteredSnapshot resourceDisplayRegisteredSnapshot();
	ResourceDisplayEffectiveSnapshot resourceDisplayEffectiveSnapshot();
	ResourceDisplayRefreshResult requestResourceDisplayRefresh();
	void setResourceDisplayEnabled(boolean enabled);
	boolean isResourceDisplayEnabled();
}
