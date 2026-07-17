package choir.internal.options;

public final class OptionsCapabilities {
	private static volatile boolean mainMenuUi;
	private static volatile boolean inGameUi;
	private static volatile String uiDetail = "not-verified";

	private OptionsCapabilities() { }
	public static boolean registrationAvailable() { return true; }
	public static boolean globalPersistenceAvailable() { return true; }
	public static boolean mainMenuUiAvailable() { return mainMenuUi; }
	public static boolean inGameUiAvailable() { return inGameUi; }
	public static String uiDetail() { return uiDetail; }
	public static synchronized void uiCompatibility(boolean available, String detail) {
		mainMenuUi = available;
		inGameUi = available;
		uiDetail = detail == null ? "<none>" : detail;
	}
}
