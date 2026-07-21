package hooks;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;
import utilities.DriverManager;
import utilities.DriverProvider;
import utilities.FileManager;
import utilities.Logs;

public class Hooks {
    private static final DriverManager driverManager = new DriverManager();

    @BeforeAll
    public static void beforeAll() {
        Logs.info("beforeAll");
        FileManager.deletePreviousEvidence();
    }

    @AfterAll
    public static void afterAll() {
        Logs.info("afterAll");
    }

    @Before
    public static void before(Scenario scenario) {
        Logs.info("before: %s", scenario.getName());
        driverManager.buildDriver();
    }

    @After
    public static void after(Scenario scenario) {
        Logs.info("after: %s, status: %s", scenario.getName(), scenario.getStatus());
        try {
            switch (scenario.getStatus()) {
                case SKIPPED, FAILED -> captureEvidence(scenario);
            }
        } finally {
            driverManager.killDriver();
        }
    }

    private static void captureEvidence(Scenario scenario) {
        if (new DriverProvider().get() == null) {
            Logs.warning("Skipping evidence capture for %s: no driver is available, driver initialization may not have completed.",
                    scenario.getName());
            return;
        }

        try {
            FileManager.getScreenshot(scenario.getName());
            FileManager.attachScreenshot(scenario);
        } catch (RuntimeException runtimeException) {
            Logs.error("Failed to capture screenshot for %s: %s", scenario.getName(), runtimeException.getLocalizedMessage());
        }

        try {
            FileManager.getPageSource(scenario.getName());
            FileManager.attachPageSource(scenario);
        } catch (RuntimeException runtimeException) {
            Logs.error("Failed to capture page source for %s: %s", scenario.getName(), runtimeException.getLocalizedMessage());
        }
    }
}
