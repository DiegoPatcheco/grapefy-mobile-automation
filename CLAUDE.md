# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Grapefy is an Android mobile BDD automation framework: Java + Appium (UiAutomator2) + Cucumber, run through the JUnit 5
Platform Suite engine. Android only — there is no iOS driver, capability path, or XCUITest config. It exercises
`wdioAPP.apk` (`src/test/resources/apk/wdioAPP.apk`), a demo app, either locally against Appium or remotely on
BrowserStack.

## Commands

Static compilation checks, no Appium or device required:

```sh
./mvnw clean compile
./mvnw test-compile
```

(Windows: `mvnw.cmd`)

Full integration build — this reaches the Failsafe `integration-test`/`verify` phases described below, so it requires a
running Appium server and an available Android emulator or device, not just compiled sources:

```sh
./mvnw clean install
```

Run the full local suite (requires a running Appium server on `http://127.0.0.1:4723/` and an available Android emulator
or connected device; the APK must exist at `src/test/resources/apk/wdioAPP.apk` — Appium installs it on the device as
part of session creation, it does not need to be pre-installed):

```sh
./runSuite.sh
# equivalent to: ./mvnw clean verify -Dgroups="regression"
```

Run a single scenario: tag it `@single` in its `.feature` file, then:

```sh
./mvnw clean verify -Dcucumber.features="src/test/resources/features" -Dcucumber.filter.tags="@single"
```

Run against BrowserStack (requires `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` set in the environment — both
scripts fail fast if either is missing):

```sh
./runSuiteBS.sh       # full suite, -Pbrowserstack -Dgroups="regression" -DRUN_ON_BROWSERSTACK=true
./runSingleTest.sh    # @single only, -Pbrowserstack -Dcucumber.filter.tags="@single" -DRUN_ON_BROWSERSTACK=true
```

Both `-Pbrowserstack` (Maven profile — adds the BrowserStack SDK dependency and attaches its Java agent) and
`-DRUN_ON_BROWSERSTACK=true` (runtime flag `DriverManager` reads) are required for a real remote run; local runs never
activate the `browserstack` profile.

No standalone lint command; there is no test-only phase separate from the integration-test build described below.

Open the last Cucumber HTML report:

```sh
./openReport.sh
```

## Architecture

**Execution pipeline.** There is no ordinary `mvn test` phase for this suite — `maven-surefire-plugin` explicitly skips
tests (`skipTests=true`) so `RunTests.java` (`src/test/java/main/RunTests.java`, a `@Suite @IncludeEngines("cucumber")`
JUnit Platform entry point) isn't run twice. Instead everything happens via `maven-failsafe-plugin` bound to
`integration-test` (runs the suite, writes results) and `verify` (reads the results and fails the build on any assertion
failure, `@Before` hook error, or other error — this is the authoritative pass/fail gate, not the Cucumber reporting
plugin's `checkBuildResult`). The Cucumber HTML report (`net.masterthought:maven-cucumber-reporting`) generates in
between, during `post-integration-test`, so it exists even when the build ultimately fails. All three run scripts invoke
`mvn clean verify` and preserve its exit code.

**Cucumber wiring.** `src/test/resources/junit-platform.properties` points Cucumber at `src/test/resources/features` and
glue packages `hooks,steps`. Feature files are tagged `@regression @smoke`; `@single` marks a scenario for isolated
local/remote runs (see `Drag.feature`).

**Driver lifecycle (`utilities` package).** `DriverManager.buildDriver()` reads `-DRUN_ON_BROWSERSTACK` to decide
between a local `AndroidDriver` (`http://127.0.0.1:4723/`, capabilities built from `wdioAPP.apk`) and a remote one (
BrowserStack hub, capabilities from `BROWSERSTACK_USERNAME`/`BROWSERSTACK_ACCESS_KEY`). `DriverProvider` holds the
driver in a `ThreadLocal`. `Hooks` (`src/test/java/hooks/Hooks.java`) builds the driver in `@Before` and tears it down
in `@After` per scenario, so add new global setup/teardown there rather than in step definitions.

**Failure evidence.** In `Hooks.after()`, any `SKIPPED`/`FAILED` scenario triggers `FileManager` to capture a
screenshot (`target/screenshots/`) and page source (`target/pageStructure/`), both also attached directly to the
Cucumber report via `scenario.attach(...)`. `FileManager.deletePreviousEvidence()` runs once in `@BeforeAll` to clear
stale evidence.

**Page Object Model.** `BasePage` (abstract) defines the `waitPageToLoad()` / `verifyPage()` contract plus shared
helpers (`find`, `findAll`, `waitForDisplayed`, `pressBack`) that fetch the driver via `DriverProvider`. Every screen
under `src/test/java/pages/` extends it. Step definitions (`src/test/java/steps/`, one class per feature) stay thin:
they parse Gherkin and delegate to Page Objects and `CommonFlows`, never touching driver/element APIs directly.

**Navigation composition.** `CommonFlows` centralizes the home → bottom-bar → target-screen sequence (e.g.
`goToLoginPage()`, `goToWebViewPage()`) so step definitions don't duplicate navigation. When adding a new screen, add a
corresponding `goToXPage()` there instead of inlining navigation in step defs.

**Gestures.** Touch interactions (`utilities/Gestures.java`) are built directly on Selenium's W3C Actions API (
`PointerInput`/`Sequence`) for tap/swipe/drag, kept independent of Page Objects so any page can reuse them (used by the
puzzle-drag and carousel-swipe scenarios).

**WebView/native context switching.** `ContextManager` switches the Appium session between `NATIVE_APP` and `WEBVIEW_*`
contexts (matched by substring against `driver.getContextHandles()`) so a scenario can assert on web content rendered
inside the app, then switch back.

**Test data.** `models/User.java` generates randomized login credentials via `javafaker`, used by the valid-login
scenario.

## CI

Three independent execution paths exist. Inspect the actual files under `.github/workflows/` before editing any of
them — they are the source of truth for triggers, action/tool versions, and steps, not this summary.

- **`static-validation.yml`** (GitHub Actions): runs `mvn clean test-compile` on pull requests to `main`, pushes to
  `main`, and `workflow_dispatch`. Compiles production and test sources only — no Appium, no emulator, no
  BrowserStack.
- **`android-mobile-smoke.yml`** (GitHub Actions): runs on `workflow_dispatch` and on pull requests that modify the
  workflow file itself (no `push` trigger, no execution on ordinary source/doc PRs). Boots a GitHub-hosted Android
  emulator, starts Appium locally, and runs only the `@single` scenario through `mvn clean verify` — local execution
  only, never BrowserStack. Uploads Failsafe/Cucumber/Appium evidence as artifacts.
- **`JenkinsFile`** (repo root): a declarative Jenkins pipeline definition that triggers on a GitHub push webhook, reads
  BrowserStack credentials from Jenkins credential bindings, runs `./runSuiteBS.sh`, and in `post { always { ... } }`
  publishes JUnit XML and the Cucumber HTML report regardless of pass/fail. It requires an actual Jenkins instance
  with matching job/credentials to execute.

## Repository guardrails

- Credentials (`BROWSERSTACK_USERNAME`, `BROWSERSTACK_ACCESS_KEY`) come only from environment variables — never
  hardcode or commit them.
- The `browserstack` Maven profile must never be active for local runs; only invoke it via the documented BrowserStack
  scripts.
- `maven-failsafe-plugin`'s `integration-test`/`verify` binding remains the authoritative pass/fail gate in CI as well
  as local runs — do not reintroduce test execution via `maven-surefire-plugin` (kept at `skipTests=true`).
- Do not claim or add iOS/XCUITest support — this framework is Android-only.
- `static-validation.yml` must stay free of Appium, Android emulator, and BrowserStack execution — it only compiles.
- `android-mobile-smoke.yml` must stay local-only: no BrowserStack credentials/secrets, no `-Pbrowserstack`, no
  `RUN_ON_BROWSERSTACK`, and it must keep executing only the `@single` scenario.
- Workflow artifact-upload steps must keep `if: always()` so Failsafe/Cucumber/Appium evidence is preserved on both
  pass and fail.
- Do not broaden either GitHub Actions workflow's trigger scope, and do not change pinned action, Node, Appium, driver,
  or Android API versions, without explicit approval and a validated run.
- Avoid hard waits (e.g. `Thread.sleep`) and broad/blanket retries in step definitions or Page Objects — use
  `BasePage`'s explicit-wait helpers instead.
- Do not change Maven dependencies, plugins, or build lifecycle bindings without explicit approval.
- Never commit or push changes without an explicit request from the user.
- When reporting work done, only report commands that were actually executed, with their real output/exit codes.
