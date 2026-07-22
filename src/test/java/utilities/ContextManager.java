package utilities;

import io.appium.java_client.android.AndroidDriver;

public class ContextManager {
    private static AndroidDriver getDriver() {
        return new DriverProvider().get();
    }

    public static void switchWebViewContext() {
        switchToContextContaining("WEBVIEW");
    }

    public static void switchNativeAppContext() {
        switchToContextContaining("NATIVE_APP");
    }

    private static void switchToContextContaining(String identifier) {
        final var driver = getDriver();
        final var contextSet = driver.getContextHandles();

        for (var context : contextSet) {
            if (context.contains(identifier)) {
                driver.context(context);
                return;
            }
        }

        final var message = String.format(
                "No Appium context containing \"%s\" was found. Available contexts: %s",
                identifier, contextSet);
        Logs.error(message);
        throw new IllegalStateException(message);
    }
}
