package utilities;

import io.cucumber.java.Scenario;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class FileManager {
    private static final String screenshotPath = "target/screenshots";
    private static final String pageStructurePath = "target/pageStructure";

    public static void getScreenshot(String screenshotName) {
        Logs.debug("Taking screenshot");
        final var screenshotFile = ((TakesScreenshot) new DriverProvider().get())
                .getScreenshotAs(OutputType.FILE);

        final var path = String.format("%s/%s.png", screenshotPath, screenshotName);

        try {
            FileUtils.copyFile(screenshotFile, new File(path));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException.getLocalizedMessage());
        }
    }

    public static void attachScreenshot(Scenario scenario) {
        Logs.debug("Attaching screenshot");
        final var screenshotFile = ((TakesScreenshot) new DriverProvider().get())
                .getScreenshotAs(OutputType.BYTES);

        scenario.attach(
                screenshotFile,
                "image/png",
                scenario.getName()
        );
    }

    public static void getPageSource(String fileName) {
        Logs.debug("Taking the page source");

        final var safeFileName = sanitizeFileName(fileName) + "_" + System.currentTimeMillis();
        final var path = String.format("%s/%s", pageStructurePath, safeFileName);
        final var file = new File(path);

        try {
            Files.createDirectories(file.getParentFile().toPath());

            final var pageSource = new DriverProvider().get().getPageSource();
            if (pageSource != null) {
                try (var fileWriter = new FileWriter(file)) {
                    fileWriter.write(Jsoup.parse(pageSource).toString());
                }
            }
        } catch (IOException ioException) {
            Logs.error("Failed to write page source to %s: %s", path, ioException.getLocalizedMessage());
            throw new RuntimeException("Failed to write page source to " + path, ioException);
        }
    }

    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static void attachPageSource(Scenario scenario) {
        Logs.debug("Attaching the page source");
        final var pageSource = new DriverProvider().get().getPageSource();
        final var parsedPageSource = Jsoup.parse(pageSource).toString();

        scenario.attach(
                parsedPageSource,
                "text/plain",
                scenario.getName()
        );
    }

    public static void deletePreviousEvidence() {
        try {
            FileUtils.deleteDirectory(new File(screenshotPath));
            FileUtils.deleteDirectory(new File(pageStructurePath));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException.getLocalizedMessage());
        }
    }
}
