package eu.the5zig.minecraft.protocol;

public class Logger {

	private static final boolean DEBUG = false;

	private Logger() {
	}

	public static void info(String message, Object... args) {
		System.out.println("[INFO] " + String.format(message, args));
	}

	public static void debug(String message, Object... args) {
		if (DEBUG)
			System.out.println("[DEBUG] " + String.format(message, args));
	}

	public static void error(String message, Object... args) {
		System.err.println("[ERROR] " + String.format(message, args));
	}

	public static void error(String message, Throwable throwable) {
		System.err.println("[ERROR] " + message);
		throwable.printStackTrace(System.err);
	}

}
