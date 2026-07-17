package choir.internal.options;

import choir.internal.ChoirDiagnostics;

final class OptionsDiagnostics {
	private OptionsDiagnostics() { }
	static void info(String message) { ChoirDiagnostics.beginValidationSession(); ChoirDiagnostics.info("OPTIONS " + message); }
	static void warn(String message) { ChoirDiagnostics.beginValidationSession(); ChoirDiagnostics.error("OPTIONS " + message); }
	static void error(String message, Throwable cause) {
		ChoirDiagnostics.beginValidationSession();
		String detail = cause == null ? "" : " :: " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
		ChoirDiagnostics.error("OPTIONS " + message + detail);
	}
}
