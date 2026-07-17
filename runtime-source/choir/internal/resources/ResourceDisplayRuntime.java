package choir.internal.resources;

import java.util.ArrayList;
import java.util.List;

import choir.api.experimental.resources.ResourceDisplayEffectiveGroup;
import choir.api.experimental.resources.ResourceDisplayEffectiveSnapshot;
import choir.api.experimental.resources.ResourceDisplayRefreshResult;
import choir.api.experimental.resources.ResourceDisplayRuntimeState;
import choir.internal.ChoirDiagnostics;
import choir.internal.resources.ResourceDisplayModel.Diagnostic;
import choir.internal.resources.ResourceDisplayModel.FallbackSection;
import choir.internal.resources.ResourceDisplayModel.Group;
import choir.internal.resources.ResourceDisplayModelBuilder.ResourceEntry;

/** Process coordinator for model state. It owns no game-specific type. */
public final class ResourceDisplayRuntime {
	private static final State PROCESS = new State();

	private ResourceDisplayRuntime() { }

	public static void adapterReady(String adapterVersion, boolean uiTargetsCompatible,
			String compatibilitySignature) {
		PROCESS.adapterReady(adapterVersion, uiTargetsCompatible, compatibilitySignature);
		ChoirDiagnostics.info("RESOURCE-DISPLAY adapter-ready adapter=" + adapterVersion
				+ " uiTargetsCompatible=" + uiTargetsCompatible
				+ " compatibility.signature=" + compatibilitySignature);
	}

	public static void adapterUnavailable(String reason) {
		PROCESS.adapterUnavailable(reason);
		ChoirDiagnostics.error("RESOURCE-DISPLAY adapter-unavailable reason=" + reason);
	}

	public static void registrationChanged() {
		PROCESS.registrationChanged(ResourceDisplayRegistry.generation());
		ChoirDiagnostics.info("RESOURCE-DISPLAY model-stale reason=registration-change registration.generation="
				+ ResourceDisplayRegistry.generation());
	}

	public static ResourceDisplayRefreshResult requestRefresh() {
		ResourceDisplayRefreshResult result = PROCESS.requestRefresh();
		ResourceDisplayEffectiveSnapshot snapshot = PROCESS.snapshot();
		ChoirDiagnostics.info("RESOURCE-DISPLAY refresh-requested result=" + result
				+ " refresh.generation=" + snapshot.refreshGeneration()
				+ " localization.generation=" + snapshot.localizationGeneration());
		return result;
	}

	public static void setEnabled(boolean enabled) {
		PROCESS.setEnabled(enabled);
		ChoirDiagnostics.info("RESOURCE-DISPLAY feature enabled=" + enabled
				+ " state=" + PROCESS.snapshot().state());
	}

	public static boolean isEnabled() { return PROCESS.isEnabled(); }
	public static ResourceDisplayEffectiveSnapshot snapshot() { return PROCESS.snapshot(); }
	public static void registryDisposed(String reason) {
		PROCESS.registryDisposed(reason);
		ChoirDiagnostics.info("RESOURCE-DISPLAY model-stale reason=" + reason + " live-registry-disposed=true");
	}

	public static void observeAndRebuild(ResourceDisplayRuntimeResolver resolver, String reason) {
		RebuildResult result = PROCESS.observeAndRebuild(resolver, ResourceDisplayRegistry.snapshot(), reason);
		ResourceDisplayEffectiveSnapshot snapshot = PROCESS.snapshot();
		if (result == RebuildResult.REBUILT) {
			ChoirDiagnostics.info("RESOURCE-DISPLAY registry-observed runtime.generation="
					+ snapshot.runtimeRegistryGeneration() + " registry.signature=" + snapshot.registrySignature()
					+ " resources=" + snapshot.canonicalResourceCount());
			for (String diagnostic : PROCESS.adapterDiagnostics())
				ChoirDiagnostics.info("RESOURCE-DISPLAY " + diagnostic);
			for (Diagnostic diagnostic : PROCESS.diagnostics())
				ChoirDiagnostics.info("RESOURCE-DISPLAY diagnostic code=" + diagnostic.code() + " " + diagnostic.detail());
			ChoirDiagnostics.info("RESOURCE-DISPLAY model-rebuilt reason=" + reason
					+ " state=" + snapshot.state() + " registration.generation=" + snapshot.registrationGeneration()
					+ " runtime.generation=" + snapshot.runtimeRegistryGeneration()
					+ " model.generation=" + snapshot.modelGeneration()
					+ " refresh.generation=" + snapshot.refreshGeneration()
					+ " localization.generation=" + snapshot.localizationGeneration()
					+ " explicit.groups=" + PROCESS.explicitGroupCount()
					+ " fallback.groups=" + PROCESS.fallbackGroupCount()
					+ " assigned.resources=" + snapshot.assignedResourceCount()
					+ " fallback.resources=" + snapshot.fallbackResourceCount()
					+ " model.signature=" + snapshot.modelSignature());
		} else if (result == RebuildResult.UNCHANGED) {
			ChoirDiagnostics.info("RESOURCE-DISPLAY model-unchanged reason=" + reason
					+ " model.generation=" + snapshot.modelGeneration()
					+ " model.signature=" + snapshot.modelSignature());
		} else if (result == RebuildResult.FAILED) {
			ChoirDiagnostics.error("RESOURCE-DISPLAY rebuild-failed reason=" + reason
					+ " detail=" + snapshot.detail());
		} else {
			ChoirDiagnostics.info("RESOURCE-DISPLAY rebuild-skipped reason=" + reason
					+ " state=" + snapshot.state());
		}
	}

	public static boolean isCurrentHandle(ResourceDisplayRuntimeHandle handle) {
		boolean current = PROCESS.isCurrentHandle(handle);
		if (!current) ChoirDiagnostics.error("RESOURCE-DISPLAY stale-handle-rejected resource="
				+ (handle == null ? "<null>" : handle.resourceId()));
		return current;
	}

	public static List<ResourceDisplayRuntimeHandle> currentHandlesForGroup(String groupId) {
		return PROCESS.currentHandlesForGroup(groupId);
	}

	enum RebuildResult { REBUILT, UNCHANGED, SKIPPED, FAILED }

	static final class State {
		private boolean adapterReady;
		private boolean enabled = true;
		private boolean uiTargetsCompatible;
		private boolean stale;
		private boolean refreshPending;
		private String adapterVersion = "";
		private String compatibilitySignature = "";
		private String detail = "Adapter has not initialized.";
		private ResourceDisplayRuntimeState runtimeState = ResourceDisplayRuntimeState.ADAPTER_UNAVAILABLE;
		private long runtimeRegistryGeneration;
		private long lastRegistrationGeneration;
		private long modelGeneration;
		private long refreshGeneration;
		private long localizationGeneration;
		private String cacheKey = "";
		private ResourceDisplayRuntimeObservation observation;
		private ResourceDisplayModel model;
		private List<Diagnostic> diagnostics = List.of();

		synchronized void adapterReady(String version, boolean compatible, String signature) {
			adapterReady = true;
			adapterVersion = version;
			uiTargetsCompatible = compatible;
			compatibilitySignature = signature;
			runtimeState = !enabled ? ResourceDisplayRuntimeState.DISABLED
					: (compatible ? ResourceDisplayRuntimeState.REGISTRY_NOT_READY : ResourceDisplayRuntimeState.ADAPTER_UNAVAILABLE);
			detail = compatible ? "Adapter ready; live registry not observed." : "Future UI target compatibility gate failed.";
		}

		synchronized void adapterUnavailable(String reason) {
			adapterReady = false;
			clearLiveState();
			runtimeState = ResourceDisplayRuntimeState.ADAPTER_UNAVAILABLE;
			detail = reason;
		}

		synchronized void registrationChanged(long registrationGeneration) {
			lastRegistrationGeneration = registrationGeneration;
			stale = true;
			if (runtimeState == ResourceDisplayRuntimeState.MODEL_READY
					|| runtimeState == ResourceDisplayRuntimeState.NO_REGISTRATIONS)
				runtimeState = ResourceDisplayRuntimeState.MODEL_STALE;
		}

		synchronized ResourceDisplayRefreshResult requestRefresh() {
			if (!enabled) return ResourceDisplayRefreshResult.DISABLED;
			if (refreshPending) return ResourceDisplayRefreshResult.COALESCED;
			refreshPending = true;
			stale = true;
			refreshGeneration++;
			localizationGeneration++;
			if (model != null) runtimeState = ResourceDisplayRuntimeState.MODEL_STALE;
			return ResourceDisplayRefreshResult.REQUESTED;
		}

		synchronized void setEnabled(boolean value) {
			if (enabled == value) return;
			enabled = value;
			refreshGeneration++;
			localizationGeneration++;
			cacheKey = "";
			model = null;
			diagnostics = List.of();
			observation = null;
			stale = value;
			refreshPending = false;
			runtimeState = value
					? (adapterReady ? ResourceDisplayRuntimeState.REGISTRY_NOT_READY : ResourceDisplayRuntimeState.ADAPTER_UNAVAILABLE)
					: ResourceDisplayRuntimeState.DISABLED;
			detail = value ? "Feature enabled; awaiting safe live-registry observation." : "Feature disabled.";
		}

		synchronized boolean isEnabled() { return enabled; }

		synchronized void registryDisposed(String reason) {
			clearLiveState();
			stale = true;
			runtimeState = !enabled ? ResourceDisplayRuntimeState.DISABLED
					: (adapterReady && uiTargetsCompatible
							? ResourceDisplayRuntimeState.REGISTRY_NOT_READY
							: ResourceDisplayRuntimeState.ADAPTER_UNAVAILABLE);
			detail = "Live registry disposed: " + reason;
		}

		synchronized RebuildResult observeAndRebuild(ResourceDisplayRuntimeResolver resolver,
				ResourceDisplayRegistry.Snapshot registrations, String reason) {
			if (!enabled) { runtimeState = ResourceDisplayRuntimeState.DISABLED; return RebuildResult.SKIPPED; }
			if (!adapterReady || !uiTargetsCompatible) {
				runtimeState = ResourceDisplayRuntimeState.ADAPTER_UNAVAILABLE;
				detail = !adapterReady ? "Adapter unavailable." : "Future UI target compatibility gate failed.";
				return RebuildResult.SKIPPED;
			}
			try {
				lastRegistrationGeneration = registrations.registrationGeneration();
				ResourceDisplayRuntimeObservation candidate = resolver.observe(runtimeRegistryGeneration + 1);
				boolean registryChanged = observation == null
						|| !observation.identity().sameRegistry(candidate.identity())
						|| !observation.registrySignature().equals(candidate.registrySignature());
				if (registryChanged) {
					runtimeRegistryGeneration++;
					if (candidate.runtimeRegistryGeneration() != runtimeRegistryGeneration)
						throw new IllegalStateException("Adapter runtime-registry generation mismatch.");
					observation = candidate;
					stale = true;
				}

				String nextKey = cacheKey(registrations.registrationGeneration(), observation,
						localizationGeneration, enabled, refreshGeneration);
				if (!stale && model != null && cacheKey.equals(nextKey)) {
					resolver.verifyUnchanged(observation);
					return RebuildResult.UNCHANGED;
				}

				List<ResourceEntry> entries = new ArrayList<ResourceEntry>(observation.resourceCount());
				for (ResourceDisplayRuntimeHandle handle : observation.handles()) {
					if (handle.observedIndex() != handle.canonicalPosition())
						throw new IllegalStateException("RESOURCE.index mismatch for " + handle.resourceId());
					entries.add(new ResourceEntry(handle.resourceId(), handle.canonicalPosition(), handle.nativeCategory()));
				}
				ResourceDisplayModel rebuilt = ResourceDisplayModelBuilder.build(modelGeneration + 1, registrations, entries);
				resolver.verifyUnchanged(observation);
				model = rebuilt;
				modelGeneration++;
				cacheKey = nextKey;
				diagnostics = rebuilt.diagnostics();
				stale = false;
				refreshPending = false;
				runtimeState = registrations.groups().isEmpty() && registrations.assignments().isEmpty()
						? ResourceDisplayRuntimeState.NO_REGISTRATIONS : ResourceDisplayRuntimeState.MODEL_READY;
				detail = runtimeState == ResourceDisplayRuntimeState.NO_REGISTRATIONS
						? "Fallback-only model ready; no external registrations." : "Effective model ready.";
				return RebuildResult.REBUILT;
			} catch (Throwable failure) {
				clearLiveState();
				runtimeState = ResourceDisplayRuntimeState.REBUILD_FAILED;
				detail = failure.getClass().getSimpleName() + ": " + failure.getMessage();
				return RebuildResult.FAILED;
			}
		}

		synchronized ResourceDisplayEffectiveSnapshot snapshot() {
			List<ResourceDisplayEffectiveGroup> groups = new ArrayList<ResourceDisplayEffectiveGroup>();
			int assigned = 0;
			int fallback = 0;
			if (model != null && (runtimeState == ResourceDisplayRuntimeState.MODEL_READY
					|| runtimeState == ResourceDisplayRuntimeState.NO_REGISTRATIONS
					|| runtimeState == ResourceDisplayRuntimeState.MODEL_STALE)) {
				for (Group group : model.groups()) {
					groups.add(new ResourceDisplayEffectiveGroup(group.groupId(), group.label(), group.localizationKey(),
							group.winningProviderId(), group.contributorIds(), group.resourceIds(), false, null));
					assigned += group.resourceIds().size();
				}
				for (FallbackSection section : model.fallbackSections()) {
					groups.add(new ResourceDisplayEffectiveGroup(section.groupId(), section.label(),
							"choir.resource_display.native_category." + section.nativeCategory(), "choir.framework",
							List.of("choir.framework"), section.resourceIds(), true, section.nativeCategory()));
					fallback += section.resourceIds().size();
				}
			}
			return new ResourceDisplayEffectiveSnapshot(runtimeState, detail, adapterVersion, uiTargetsCompatible,
					lastRegistrationGeneration, runtimeRegistryGeneration, modelGeneration, refreshGeneration,
					localizationGeneration, observation == null ? "" : observation.registrySignature(),
					model == null ? "" : model.contentSignature(), observation == null ? 0 : observation.resourceCount(),
					assigned, fallback, groups);
		}

		synchronized boolean isCurrentHandle(ResourceDisplayRuntimeHandle handle) {
			if (handle == null || observation == null) return false;
			return handle.runtimeRegistryGeneration() == observation.runtimeRegistryGeneration()
					&& handle.registrySignature().equals(observation.registrySignature())
					&& observation.handle(handle.resourceId()) == handle;
		}

		synchronized List<ResourceDisplayRuntimeHandle> currentHandlesForGroup(String groupId) {
			if (model == null || observation == null
					|| (runtimeState != ResourceDisplayRuntimeState.MODEL_READY
							&& runtimeState != ResourceDisplayRuntimeState.NO_REGISTRATIONS)) return List.of();
			List<String> ids = null;
			for (Group group : model.groups()) if (group.groupId().equals(groupId)) ids = group.resourceIds();
			if (ids == null) for (FallbackSection section : model.fallbackSections())
				if (section.groupId().equals(groupId)) ids = section.resourceIds();
			if (ids == null) return List.of();
			List<ResourceDisplayRuntimeHandle> handles = new ArrayList<ResourceDisplayRuntimeHandle>(ids.size());
			for (String id : ids) {
				ResourceDisplayRuntimeHandle handle = observation.handle(id);
				if (handle == null || !isCurrentHandle(handle)) throw new IllegalStateException("Effective model has stale handle: " + id);
				handles.add(handle);
			}
			return List.copyOf(handles);
		}

		synchronized List<Diagnostic> diagnostics() { return diagnostics; }
		synchronized List<String> adapterDiagnostics() {
			return observation == null ? List.of() : observation.adapterDiagnostics();
		}
		synchronized int explicitGroupCount() { return model == null ? 0 : model.groups().size(); }
		synchronized int fallbackGroupCount() { return model == null ? 0 : model.fallbackSections().size(); }

		private void clearLiveState() {
			observation = null;
			model = null;
			diagnostics = List.of();
			cacheKey = "";
			stale = false;
			refreshPending = false;
		}

		private static String cacheKey(long registrationGeneration, ResourceDisplayRuntimeObservation observed,
				long localizationGeneration, boolean enabled, long refreshGeneration) {
			return registrationGeneration + "|" + observed.runtimeRegistryGeneration() + "|"
					+ observed.registrySignature() + "|"
					+ observed.identity().description() + "|" + localizationGeneration + "|"
					+ enabled + "|" + refreshGeneration;
		}
	}
}
