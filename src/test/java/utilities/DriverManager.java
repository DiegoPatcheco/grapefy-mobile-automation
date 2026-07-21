package utilities;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class DriverManager {
    private final boolean runOnBrowserstack =
            "true".equalsIgnoreCase(System.getProperty("RUN_ON_BROWSERSTACK"));

    public void buildDriver() {
        if (runOnBrowserstack) {
            Logs.debug("RUN_ON_BROWSERSTACK is TRUE. Initializing remote driver.");
            buildRemoteDriver();
        } else {
            Logs.debug("RUN_ON_BROWSERSTACK is FALSE. Initializing local driver");
            buildLocalDriver();
        }
    }

    public void killDriver() {
        Logs.debug("Killing driver");
        new DriverProvider().get().quit();
    }

    private void buildLocalDriver() {
        try {
            final var appiumURL = "http://127.0.0.1:4723/";
            final var desiredCapabilities = getDesiredLocalCapabilities();

            Logs.debug("Initializing driver");
            final var driver = new AndroidDriver(new URL(appiumURL), desiredCapabilities);

            Logs.debug("Assign driver to driver provider");
            new DriverProvider().set(driver);
        } catch (MalformedURLException malformedURLException) {
            throw new RuntimeException(malformedURLException.getLocalizedMessage());
        }
    }

    private void buildRemoteDriver() {
        try {
            final var browserStackURL = "https://hub.browserstack.com/wd/hub";
            final var mutableCapabilities = getMutableRemoteCapabilities();

            Logs.debug("Initializing driver");
            final var driver = new AndroidDriver(new URL(browserStackURL), mutableCapabilities);

            Logs.debug("Assign driver to driver provider");
            new DriverProvider().set(driver);
        } catch (MalformedURLException malformedURLException) {
            throw new RuntimeException(malformedURLException.getLocalizedMessage());
        }
    }

    private static DesiredCapabilities getDesiredLocalCapabilities() {
        final var desiredCapabilities = new DesiredCapabilities();

        final var fileAPK = new File("src/test/resources/apk/wdioAPP.apk");

        desiredCapabilities.setCapability("appium:autoGrantPermissions", true);
        desiredCapabilities.setCapability("appium:appWaitActivity", "com.wdiodemoapp.MainActivity");
        desiredCapabilities.setCapability("appium:platformName", "Android");
        desiredCapabilities.setCapability("appium:automationName", "UiAutomator2");
        desiredCapabilities.setCapability("appium:app", fileAPK.getAbsolutePath());

        return desiredCapabilities;
    }

    private static MutableCapabilities getMutableRemoteCapabilities() {
        final var mutableCapabilities = new MutableCapabilities();

        final var userName = System.getenv("BROWSERSTACK_USERNAME");
        final var accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");

        if (userName == null || userName.isBlank() || accessKey == null || accessKey.isBlank()) {
            throw new IllegalStateException(
                    "Remote execution requires the BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY "
                            + "environment variables to be set.");
        }

        HashMap<String, String> bstackOptions = new HashMap<>();
        bstackOptions.putIfAbsent("source", "cucumber:appium-intellij:v1.1.6");
        bstackOptions.putIfAbsent("deviceName", "Samsung Galaxy S22 Ultra");
        bstackOptions.putIfAbsent("platformVersion", "12.0");
        bstackOptions.putIfAbsent("platformName", "android");
        bstackOptions.putIfAbsent("projectName", "Grapefy");
        bstackOptions.putIfAbsent("buildName", "run-suite build");
        bstackOptions.putIfAbsent("appium:app", "bs://a56592460559411ef05872a72ecdb777b6ba2082");
        bstackOptions.putIfAbsent("userName", userName);
        bstackOptions.putIfAbsent("accessKey", accessKey);

        return mutableCapabilities;
    }
}
