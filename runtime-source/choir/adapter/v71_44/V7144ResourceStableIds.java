package choir.adapter.v71_44;

/**
 * V71.44 resource-ID policy. Leading-underscore registry keys are internal
 * fixed-record keys; KeyMap.expand() publishes the same object under the
 * public alias without the underscore.
 */
final class V7144ResourceStableIds {
	private V7144ResourceStableIds() { }

	static String publicAliasCandidate(String internalKey) {
		if (internalKey == null || internalKey.isEmpty())
			throw new IllegalArgumentException("Internal resource key must not be blank.");
		return internalKey.length() > 1 && internalKey.charAt(0) == '_'
				? internalKey.substring(1) : internalKey;
	}

	static String canonical(String internalKey, boolean publicAliasMapsSameObject) {
		String candidate = publicAliasCandidate(internalKey);
		return publicAliasMapsSameObject ? candidate : internalKey;
	}

	static String aliasDiagnostic(String canonicalId, String internalKey, int canonicalPosition,
			int observedIndex, int nativeCategory, boolean nativeMapResolved, int liveObjectIdentity) {
		return "stable-id-alias resource=" + canonicalId + " internal.key=" + internalKey
				+ " canonical.position=" + canonicalPosition + " index=" + observedIndex
				+ " category=" + nativeCategory + " canonical-resolved=true"
				+ " native-map-resolved=" + nativeMapResolved + " object.identity=" + liveObjectIdentity;
	}

	static String mapGapDiagnostic(String canonicalId) {
		return "native-map-gap resource=" + canonicalId
				+ " canonical-resolved=true native-map-resolved=false";
	}
}
