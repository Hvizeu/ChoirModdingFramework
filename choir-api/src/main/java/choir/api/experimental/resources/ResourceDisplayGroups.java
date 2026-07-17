package choir.api.experimental.resources;

import choir.api.spi.ChoirRuntimeServices;

/**
 * Experimental registration facade. V71.44 currently consumes the model only
 * in the right-side settlement resource panel; stockpile selectors remain vanilla.
 */
public final class ResourceDisplayGroups {
	private ResourceDisplayGroups() { }

	public static ResourceDisplayRegistrationResult registerGroup(ResourceDisplayGroupDefinition definition) {
		return ChoirRuntimeServices.require().registerResourceDisplayGroup(definition);
	}

	public static ResourceDisplayRegistrationResult registerAssignment(ResourceDisplayAssignment assignment) {
		return ChoirRuntimeServices.require().registerResourceDisplayAssignment(assignment);
	}

	/** Increases only when a new definition or assignment is accepted. */
	public static long registrationGeneration() { return ChoirRuntimeServices.require().resourceDisplayRegistrationGeneration(); }

	public static ResourceDisplayRegisteredSnapshot registeredSnapshot() {
		return ChoirRuntimeServices.require().resourceDisplayRegisteredSnapshot();
	}

	public static ResourceDisplayEffectiveSnapshot effectiveSnapshot() {
		return ChoirRuntimeServices.require().resourceDisplayEffectiveSnapshot();
	}

	public static ResourceDisplayRefreshResult requestRefresh() {
		return ChoirRuntimeServices.require().requestResourceDisplayRefresh();
	}

	public static void setEnabled(boolean enabled) { ChoirRuntimeServices.require().setResourceDisplayEnabled(enabled); }
	public static boolean isEnabled() { return ChoirRuntimeServices.require().isResourceDisplayEnabled(); }
}
