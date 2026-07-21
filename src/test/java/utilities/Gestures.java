package utilities;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.List;

public class Gestures {
    private static final PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

    private static AndroidDriver getDriver() {
        return new DriverProvider().get();
    }

    public static void tap(WebElement element) {
        final var pointCenter = getCenterPoint(element);
        final var sequence = new Sequence(finger, 1);

        Logs.debug("Move finger to element");
        sequence.addAction(
                finger.createPointerMove(
                        Duration.ZERO,
                        PointerInput.Origin.viewport(),
                        pointCenter
                )
        );

        Logs.debug("Press finger to element");
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

        Logs.debug("Hold finger 1s");
        sequence.addAction(new Pause(finger, Duration.ofMillis(100)));

        Logs.debug("Release finger from element");
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        Logs.debug("Perform action");
        getDriver().perform(List.of(sequence));
    }

    public static void dragTo(WebElement origin, WebElement destiny) {
        final var originPointCenter = getCenterPoint(origin);
        final var destinyPointCenter = getCenterPoint(destiny);
        final var sequence = new Sequence(finger, 1);

        Logs.debug("Move finger to origin");
        sequence.addAction(
                finger.createPointerMove(
                        Duration.ofMillis(200),
                        PointerInput.Origin.viewport(),
                        originPointCenter
                )
        );

        Logs.debug("Press finger");
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

        Logs.debug("Hold finger 1s");
        sequence.addAction(new Pause(finger, Duration.ofMillis(100)));

        Logs.debug("Move finger to destiny");
        sequence.addAction(
                finger.createPointerMove(
                        Duration.ofMillis(400),
                        PointerInput.Origin.viewport(),
                        destinyPointCenter
                )
        );

        Logs.debug("Hold finger 1s");
        sequence.addAction(new Pause(finger, Duration.ofMillis(100)));

        Logs.debug("Release finger");
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        Logs.debug("Perform action");
        getDriver().perform(List.of(sequence));
    }

    public static void swipe(
            double xOriginPercentage,
            double yOriginPercentage,
            double xDestinyPercentage,
            double yDestinyPercentage,
            WebElement element
    ) {
        final var originPoint =
                getElementPointWithPercentages(xOriginPercentage, yOriginPercentage, element);
        final var destinyPoint =
                getElementPointWithPercentages(xDestinyPercentage, yDestinyPercentage, element);

        swipePoints(originPoint, destinyPoint);
    }

    public static void horizontalSwipe(
            double yPercentage,
            double xOriginPercentage,
            double xDestinyPercentage,
            WebElement element
    ) {
        swipe(xOriginPercentage, yPercentage, xDestinyPercentage, yPercentage, element);
    }

    public static void verticalSwipe(
            double xPercentage,
            double yOriginPercentage,
            double yDestinyPercentage,
            WebElement element
    ) {
        swipe(xPercentage, yOriginPercentage, xPercentage, yDestinyPercentage, element);
    }

    private static void swipePoints(Point origin, Point destiny) {
        final var sequence = new Sequence(finger, 1);

        Logs.debug("Move finger to origin");
        sequence.addAction(
                finger.createPointerMove(
                        Duration.ZERO,
                        PointerInput.Origin.viewport(),
                        origin
                )
        );

        Logs.debug("Press finger");
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));

        Logs.debug("Hold finger");
        sequence.addAction(new Pause(finger, Duration.ofMillis(150)));

        Logs.debug("Move finger to destiny");
        sequence.addAction(
                finger.createPointerMove(
                        Duration.ofMillis(500),
                        PointerInput.Origin.viewport(),
                        destiny
                )
        );

        Logs.debug("Release finger");
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        Logs.debug("Perform action");
        getDriver().perform(List.of(sequence));
    }

    private static Point getElementPointWithPercentages(double xPercentage, double yPercentage, WebElement element) {
        final var location = element.getLocation();
        final var size = element.getSize();

        final var xDelta = (xPercentage / 100) * size.getWidth();
        final var yDelta = (yPercentage / 100) * size.getHeight();

        final var x = (int) (location.getX() + xDelta);
        final var y = (int) (location.getY() + yDelta);

        return new Point(x, y);
    }

    private static Point getCenterPoint(WebElement element) {
        final var elementLocation = element.getLocation();
        final var elementSize = element.getSize();

        final var xCenter = elementLocation.getX() + elementSize.getWidth() / 2;
        final var yCenter = elementLocation.getY() + elementSize.getHeight() / 2;

        return new Point(xCenter, yCenter);
    }
}
