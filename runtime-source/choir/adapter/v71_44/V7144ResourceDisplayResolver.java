package choir.adapter.v71_44;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import choir.internal.resources.ResourceDisplayRegistryIdentity;
import choir.internal.resources.ResourceDisplayRuntime;
import choir.internal.resources.ResourceDisplayRuntimeHandle;
import choir.internal.resources.ResourceDisplayRuntimeObservation;
import choir.internal.resources.ResourceDisplayRuntimeResolver;
import game.GAME;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import settlement.main.SETT;
import snake2d.util.sets.LIST;

/** Direct V71.44 live-registry observer. It is the only layer retaining RESOURCE references. */
final class V7144ResourceDisplayResolver implements ResourceDisplayRuntimeResolver {
	@Override
	public ResourceDisplayRuntimeObservation observe(long proposedRuntimeRegistryGeneration) {
		LIST<RESOURCE> all = RESOURCES.ALL();
		SETT settlement = GAME.s();
		RESOURCE[] live = new RESOURCE[all.size()];
		List<String> ids = new ArrayList<String>(all.size());
		List<Integer> indices = new ArrayList<Integer>(all.size());
		List<Integer> categories = new ArrayList<Integer>(all.size());
		List<String> adapterDiagnostics = new ArrayList<String>();
		Set<String> duplicateGuard = new HashSet<String>();
		var nativeMap = RESOURCES.map();
		for (int position = 0; position < all.size(); position++) {
			RESOURCE resource = all.get(position);
			String internalKey = resource.key();
			RESOURCE nativeByInternalKey = nativeMap.tryGet(internalKey);
			String publicAliasCandidate = V7144ResourceStableIds.publicAliasCandidate(internalKey);
			RESOURCE nativeByPublicAlias = nativeMap.tryGet(publicAliasCandidate);
			boolean publicAliasMapsSameObject = !internalKey.equals(publicAliasCandidate)
					&& nativeByPublicAlias == resource;
			String id = V7144ResourceStableIds.canonical(internalKey, publicAliasMapsSameObject);
			if (!duplicateGuard.add(id)) throw new IllegalStateException("Duplicate live resource ID: " + id);
			RESOURCE nativeByCanonicalId = id.equals(publicAliasCandidate)
					? nativeByPublicAlias : nativeByInternalKey;
			if (nativeByInternalKey != null && nativeByInternalKey != resource)
				throw new IllegalStateException("Internal resource key maps to a different live object: " + internalKey);
			if (nativeByCanonicalId != null && nativeByCanonicalId != resource)
				throw new IllegalStateException("Canonical resource ID maps to a different live object: " + id);
			boolean nativeMapResolved = nativeByCanonicalId == resource;
			if (!internalKey.equals(id))
				adapterDiagnostics.add(V7144ResourceStableIds.aliasDiagnostic(id, internalKey, position,
						resource.index(), resource.category, nativeMapResolved, System.identityHashCode(resource)));
			if (!nativeMapResolved)
				adapterDiagnostics.add(V7144ResourceStableIds.mapGapDiagnostic(id));
			if (Boolean.getBoolean("choir.resourceDisplay.debugRegistry"))
				adapterDiagnostics.add("registry-entry position=" + position + " index=" + resource.index()
						+ " resource=" + id + " internal.key=" + internalKey + " category=" + resource.category
						+ " public-alias.candidate=" + publicAliasCandidate
						+ " native-map.internal.same=" + (nativeByInternalKey == resource)
						+ " native-map.public-alias.same=" + (nativeByPublicAlias == resource)
						+ " object.identity=" + System.identityHashCode(resource));
			live[position] = resource;
			ids.add(id);
			indices.add(resource.index());
			categories.add(resource.category);
		}
		String idSignature = stringSignature(ids);
		String indexSignature = integerSignature(indices);
		String categorySignature = integerSignature(categories);
		String registrySignature = sha256((all.size() + "|" + idSignature + "|" + indexSignature + "|"
				+ categorySignature).getBytes(StandardCharsets.UTF_8));
		Identity identity = new Identity(settlement, all, live);
		List<Handle> handles = new ArrayList<Handle>(live.length);
		for (int position = 0; position < live.length; position++)
			handles.add(new Handle(live[position], ids.get(position), position, live[position].index(), live[position].category,
					proposedRuntimeRegistryGeneration, registrySignature));
		return new ResourceDisplayRuntimeObservation(proposedRuntimeRegistryGeneration, identity, registrySignature,
				idSignature, indexSignature, categorySignature, handles, adapterDiagnostics);
	}

	@Override
	public void verifyUnchanged(ResourceDisplayRuntimeObservation expected) {
		ResourceDisplayRuntimeObservation actual = observe(expected.runtimeRegistryGeneration());
		if (!expected.identity().sameRegistry(actual.identity())
				|| expected.resourceCount() != actual.resourceCount()
				|| !expected.registrySignature().equals(actual.registrySignature())
				|| !expected.orderedIdSignature().equals(actual.orderedIdSignature())
				|| !expected.orderedIndexSignature().equals(actual.orderedIndexSignature())
				|| !expected.nativeCategorySignature().equals(actual.nativeCategorySignature()))
			throw new IllegalStateException("Canonical live resource registry changed during display-model construction.");
	}

	RESOURCE liveResource(ResourceDisplayRuntimeHandle handle) {
		if (!(handle instanceof Handle current) || !ResourceDisplayRuntime.isCurrentHandle(handle))
			throw new IllegalStateException("Stale or foreign V71.44 resource-display handle.");
		return current.resource;
	}

	private static String stringSignature(List<String> values) {
		return sha256(String.join("\u0000", values).getBytes(StandardCharsets.UTF_8));
	}

	private static String integerSignature(List<Integer> values) {
		StringBuilder text = new StringBuilder();
		for (Integer value : values) text.append(value).append('\u0000');
		return sha256(text.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static String sha256(byte[] bytes) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
			StringBuilder out = new StringBuilder();
			for (byte b : digest) out.append(String.format("%02x", b & 255));
			return out.toString();
		} catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
	}

	private static final class Handle implements ResourceDisplayRuntimeHandle {
		private final RESOURCE resource;
		private final String resourceId;
		private final int canonicalPosition;
		private final int observedIndex;
		private final int nativeCategory;
		private final long runtimeRegistryGeneration;
		private final String registrySignature;

		Handle(RESOURCE resource, String resourceId, int canonicalPosition, int observedIndex, int nativeCategory,
				long runtimeRegistryGeneration, String registrySignature) {
			this.resource = resource;
			this.resourceId = resourceId;
			this.canonicalPosition = canonicalPosition;
			this.observedIndex = observedIndex;
			this.nativeCategory = nativeCategory;
			this.runtimeRegistryGeneration = runtimeRegistryGeneration;
			this.registrySignature = registrySignature;
		}

		public String resourceId() { return resourceId; }
		public int canonicalPosition() { return canonicalPosition; }
		public int observedIndex() { return observedIndex; }
		public int nativeCategory() { return nativeCategory; }
		public long runtimeRegistryGeneration() { return runtimeRegistryGeneration; }
		public String registrySignature() { return registrySignature; }
		public int liveObjectIdentityToken() { return System.identityHashCode(resource); }
	}

	private static final class Identity implements ResourceDisplayRegistryIdentity {
		private final SETT settlement;
		private final LIST<RESOURCE> all;
		private final RESOURCE[] resources;

		Identity(SETT settlement, LIST<RESOURCE> all, RESOURCE[] resources) {
			this.settlement = settlement;
			this.all = all;
			this.resources = resources;
		}

		public boolean sameRegistry(ResourceDisplayRegistryIdentity other) {
			if (!(other instanceof Identity value) || settlement != value.settlement || all != value.all
					|| resources.length != value.resources.length) return false;
			for (int i = 0; i < resources.length; i++) if (resources[i] != value.resources[i]) return false;
			return true;
		}

		public String description() {
			StringBuilder value = new StringBuilder("sett=").append(System.identityHashCode(settlement))
					.append(",all=").append(System.identityHashCode(all)).append(",resources=");
			for (RESOURCE resource : resources) value.append(System.identityHashCode(resource)).append('.');
			return sha256(value.toString().getBytes(StandardCharsets.UTF_8));
		}
	}
}
