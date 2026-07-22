# Grapefy — Android Mobile BDD Automation Framework

I created Grapefy around a simple idea: automated tests should help explain product behavior, not make failures harder to understand. This repository is an Android mobile automation framework built with Java, Appium, and Cucumber, structured the way I'd structure a real test suite at work — Page Object Model, thin step definitions, reusable navigation flows, and evidence capture on failure, rather than a pile of scripts that only I can read.

It's meant to demonstrate how I approach mobile test automation: clear separation between test intent (Gherkin), test logic (step definitions), and UI interaction (Page Objects), plus the supporting pieces — driver lifecycle management, reporting, and CI/CD wiring — that turn a test suite into something a team can actually run and trust.

## Current scope

- **Android automation only.** The driver layer is built around `AndroidDriver` and UiAutomator2; there is no iOS driver, capability path, or XCUITest configuration in this repository.
- **Local execution** through a locally running Appium server and an Android emulator or physical device.
- **Remote execution configuration** for BrowserStack (`browserstack.yml`, dedicated run scripts, and a Jenkins pipeline).
- iOS is not currently implemented. See [Roadmap](#roadmap).

## What this project demonstrates

- Java and Appium Android automation, using the UiAutomator2 automation engine.
- Cucumber/Gherkin BDD, executed through the JUnit 5 Platform Suite engine (`cucumber-junit-platform-engine`).
- Page Object Model: an abstract `BasePage` contract (`waitPageToLoad()` / `verifyPage()`) implemented by one Page Object per app screen.
- Mobile gestures built directly on W3C Actions (`PointerInput`/`Sequence`) — tap, swipe, and drag, used for the puzzle-drag and carousel-swipe scenarios.
- WebView and native-context handling: switching the Appium session between `NATIVE_APP` and `WEBVIEW_*` contexts to assert on web content rendered inside the app.
- Reusable utilities: navigation composition (`CommonFlows`), per-scenario driver lifecycle (`DriverManager`/`DriverProvider`), and automatic screenshot + page-source capture on failed or skipped scenarios (`FileManager`, wired through Cucumber hooks).
- Cucumber HTML reporting via `net.masterthought:maven-cucumber-reporting`.
- A Jenkins pipeline definition (`JenkinsFile`) configured to run the suite against BrowserStack and publish results.
- A BrowserStack configuration (`browserstack.yml`) for cloud device execution, with credentials read from environment variables rather than committed to the file.

## Tech stack

- Java 17
- Appium Java Client 9.4.0
- Selenium Java 4.29.0
- Cucumber 7.21.1 (`cucumber-java`, `cucumber-junit-platform-engine`)
- JUnit 5 (Jupiter + Platform Suite)
- Maven, via the Maven Wrapper
- Log4j2 2.24.3
- `net.masterthought:maven-cucumber-reporting` / `cucumber-reporting`
- javafaker 1.0.2 (random login credentials for the valid-login scenario)
- BrowserStack Java SDK 1.30.8

## Project structure

```
src/
├── main/
│   └── resources/
│       └── log4j2.xml              # logging configuration
└── test/
    ├── java/
    │   ├── hooks/                  # Cucumber lifecycle hooks (Hooks.java)
    │   ├── main/                   # RunTests — JUnit Platform Cucumber suite entry point
    │   ├── models/                 # User — generates randomized login credentials via javafaker
    │   ├── pages/                  # Page Objects (one per app screen)
    │   ├── steps/                  # Step definitions (one class per feature)
    │   └── utilities/              # BasePage, DriverManager, DriverProvider,
    │                                # CommonFlows, Gestures, ContextManager,
    │                                # FileManager, Logs
    └── resources/
        ├── apk/                    # wdioAPP.apk (app under test)
        └── features/                # Login, Home, Forms, Swipe, WebView, Drag
```

## Test coverage

Six feature files, all tagged `@regression @smoke`:

| Feature | Scenarios |
|---|---|
| Login | 3 scenarios + 2 Scenario Outlines (5 and 3 example rows) |
| Home | 2 scenarios |
| Forms | 3 scenarios |
| Swipe | 3 scenarios |
| WebView | 1 scenario |
| Drag | 2 scenarios |

One scenario in `Drag.feature` carries an additional `@single` tag, used to run it in isolation locally or remotely.

## Prerequisites

- JDK 17 (developed against Amazon Corretto 17.0.14)
- [Maven](https://maven.apache.org/install.html) — optional, the Maven Wrapper is included
- Git
- A running Appium server
- An Android emulator or physical device
- The app under test, `wdioAPP.apk`, already at `src/test/resources/apk/`

## Environment variables

Remote (BrowserStack) execution reads two variables directly from the environment:

```
BROWSERSTACK_USERNAME
BROWSERSTACK_ACCESS_KEY
```

Set them in your shell or CI credential store before running a BrowserStack script — do not put real values in this README, in a committed file, or in a `.env` file checked into the repository.

## Installation

```sh
git clone https://github.com/DiegoPatcheco/grapefy-mobile-automation.git
cd grapefy-mobile-automation
./mvnw clean install
```

Windows users can run `mvnw.cmd` instead of `./mvnw`.

## Local execution

Start the Appium server. First time on a machine:

```sh
appium server --allow-insecure chromedriver_autodownload
```

Subsequent runs:

```sh
appium server
```

Then start an Android emulator or connect a physical device with `wdioAPP.apk` installed.

Run the full local suite:

```sh
./runSuite.sh
```

which runs:

```sh
./mvnw clean verify -Dgroups="regression"
```

Run a single scenario: tag it `@single` in its `.feature` file, then:

```sh
./mvnw clean verify -Dcucumber.feature="src/test/resources/features" -Dcucumber.filter.tags="@single"
```

Local runs never activate the `browserstack` Maven profile, so the BrowserStack Java agent is not copied or loaded — local execution only ever talks to the Appium server above.

## BrowserStack execution

With `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` already set in your shell:

```sh
./runSuiteBS.sh       # full suite:   mvn clean verify -Pbrowserstack -Dgroups="regression" -DRUN_ON_BROWSERSTACK=true
./runSingleTest.sh    # @single only: mvn clean verify -Pbrowserstack -Dcucumber.feature="src/test/resources/features" -Dcucumber.filter.tags="@single" -DRUN_ON_BROWSERSTACK=true
```

Both flags are required for a real BrowserStack run: `-Pbrowserstack` explicitly activates the Maven profile that adds the BrowserStack SDK dependency and attaches its Java agent to the test JVM, while `-DRUN_ON_BROWSERSTACK=true` is the separate runtime flag `DriverManager` reads to build a remote driver instead of a local one. Both scripts fail fast with a clear error if either credential variable is missing, rather than attempting a session without credentials.

## Reporting and build behavior

- Mobile Cucumber/Appium scenarios run as integration tests via `maven-failsafe-plugin`, during the `integration-test` phase.
- The Cucumber HTML report is generated during `post-integration-test`, after the suite has run and before the final result is verified.
- `failsafe:verify`, bound to the `verify` phase, evaluates the recorded results and fails the build for any assertion failure, `@Before` hook error, or other test error.
- All three official scripts (`runSuite.sh`, `runSuiteBS.sh`, `runSingleTest.sh`) invoke `mvn clean verify` and preserve Maven's final exit status: a passing run exits `0`; a run with any failure or error exits non-zero.
- Both the Failsafe XML results (`target/failsafe-reports/`) and the Cucumber HTML report (`target/cucumber-html-reports/overview-features.html`) are written before that final check, so they remain available for diagnosis even when the build fails.

## Jenkins pipeline

The `JenkinsFile` at the repository root defines a declarative pipeline that:

- Triggers on a GitHub push webhook.
- Reads `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` from Jenkins credential bindings, not from hardcoded values.
- Checks out the repository and runs `./runSuiteBS.sh` against BrowserStack.
- In a `post { always { ... } }` block, publishes JUnit-compatible XML from `target/failsafe-reports/*.xml` and the Cucumber HTML report from `target/cucumber-html-reports/`, both attempted regardless of whether the test stage passed or failed.

This is a pipeline **definition** included in the repository; it requires an actual Jenkins instance with the corresponding job and credentials configured to run.

## Design decisions

- **Page Object Model**: an abstract `BasePage` defines the `waitPageToLoad()` / `verifyPage()` contract; every screen gets its own Page Object implementing it.
- **Thin step definitions**: step-definition classes parse Gherkin steps and delegate to Page Objects and `CommonFlows`, rather than containing interaction logic themselves.
- **Per-scenario driver isolation**: `DriverProvider` holds the `AndroidDriver` in a `ThreadLocal`, built fresh by `DriverManager` in a `@Before` hook and torn down in `@After`.
- **Centralized navigation**: `CommonFlows` composes the home → bottom bar → target-screen sequence once, instead of duplicating navigation across step definitions.
- **Automatic failure evidence**: the `@After` hook captures a screenshot and the page source on `FAILED`/`SKIPPED` scenarios and attaches both to the Cucumber report.
- **Gestures as a separate layer**: touch interactions (`Gestures`) are built directly on Selenium's W3C Actions API and kept independent of Page Objects, so any page can reuse them.

## Known limitations

- Android only — no iOS support.
- Local execution requires a running Appium server and an Android emulator or physical device.
- Jenkins execution requires an actual Jenkins instance with this pipeline configured.
- BrowserStack execution requires valid BrowserStack account credentials.

## Roadmap

Possible future work — none of the following is implemented today:

- Real iOS support via XCUITest.
- A GitHub Actions workflow.
- Richer failure evidence (video, more detailed captures).
- Parallel execution across multiple cloud devices.
- Configurable execution profiles (e.g. per-environment capability sets).

## Author

Diego Pacheco Flores — [github.com/DiegoPatcheco](https://github.com/DiegoPatcheco)

I spend more time thinking about why a test failed than about how green the last run looked — that's the habit this repository is meant to reflect.
