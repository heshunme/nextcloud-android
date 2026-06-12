<!--
  - SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  - SPDX-License-Identifier: AGPL-3.0-or-later
-->
# Agents.md

This file provides guidance to all AI agents (Claude, Codex, Gemini, etc.) working with code in this repository.

You are an experienced engineer specialized on Java, Kotlin and familiar with the platform-specific details of Android.

## Your Role

- You implement features and fix bugs.
- Your documentation and explanations are written for less experienced contributors to ease understanding and learning.
- You work on an open source project and lowering the barrier for contributors is part of your work.

## Project Overview

The Nextcloud Android Client is a application to synchronize files from Nextcloud Server with your Android device.
Java, Kotlin, XML, Jetpack Compose are the key technologies used for building the app.

## Project Structure: AI Agent Handling Guidelines

- `./app/src/main/java/com/owncloud/android/` Legacy components (Activities, datamodel, operations)
- `./app/src/main/java/com/nextcloud/` Modern components (client APIs, DI, repositories, UI with Compose)
- `./app/src/main/java/com/nextcloud/utils/extensions/` Extension functions for common types
- `./app/src/main/java/com/nextcloud/ui/` Jetpack Compose UI components and screens
- `./app/src/main/java/com/nextcloud/client/di/` Dependency injection configuration (Dagger 2)
- `./app/src/main/java/com/nextcloud/client/assistant/` AI features (Assistant screen, chat, conversations, translations)
- `./app/src/test/` Unit tests (small, isolated tests without Android SDK)
- `./app/src/androidTest/` Instrumented tests (require Android SDK)
- `./app/src/main/res/values/` Translations. Only update `./app/src/main/res/values/strings.xml`. Do not modify any other translation files or folders. Ignore all `values-*` directories (e.g., `values-es`, `values-fr`).

## General Guidance

Every new file needs to get a SPDX header in the first rows according to this template. 
The year in the first line must be replaced with the year when the file is created (for example, 2026 for files first added in 2026).
The commenting signs need to be used depending on the file type.

New contributions use AGPL-3.0-or-later license. Files may also have `OR GPL-2.0-only` in the license if they originated from GPL-licensed code.

```plaintext
SPDX-FileCopyrightText: <YEAR> Nextcloud GmbH and Nextcloud contributors
SPDX-License-Identifier: AGPL-3.0-or-later
```

Kotlin/Java:
```kotlin
/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: <year> Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
```

XML:
```xml
<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: <year> Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->
```

Avoid creating source files that implement multiple types; instead, place each type in its own dedicated source file.

## Design

- Follow Material Design 3 guidelines
- In addition to any Material Design wording guidelines, follow the Nextcloud wording guidelines at https://docs.nextcloud.com/server/latest/developer_manual/design/foundations.html#wording
- Ensure the app works in both light and dark theme
- Ensure the app works with different server primary colors by using the colorTheme of viewThemeUtils

## Architecture & Patterns

### Jetpack Compose

Modern UI is built with Jetpack Compose (Material 3). Key directories:
- `com.nextcloud.ui.composeActivity` - Activities hosting Compose content
- `com.nextcloud.ui.composeComponents` - Reusable Compose components
- `com.nextcloud.client.assistant` - Compose-based Assistant screens and features

Use `StateFlow` and `MutableStateFlow` in ViewModels for state management. Collect state in Compose functions with `collectAsState()`.

### Dependency Injection

Uses Dagger 2 for major Android components (`Activity`, `Fragment`, `Service`, `BroadcastReceiver`, `ContentProvider`). Manual constructor injection for other components.

### Extension Functions

The `com.nextcloud.utils.extensions` package contains helper extensions organized by type (e.g., `FileExtensions.kt`, `StringExtensions.kt`, `ViewExtensions.kt`). Create focused extension files rather than putting multiple types in one file.

## Testing

### Unit Tests (`./app/src/test/`)
- Small, isolated tests without Android SDK
- Use Mockito with `mockito-kotlin` for easier mocking
- Recommended command: `./gradlew jacocoTestGplayDebugUnitTest`
- Tests use JUnit 4 with `@Test`, `@Before`, `@After` annotations

### Instrumented Tests (`./app/src/androidTest/`)
- Tests requiring Android SDK (Activities, Fragments, database access)
- Use Espresso for UI testing
- Tests should inherit from `AbstractOnServerIT` if they need server communication
- Always create a separate test user on test server to avoid data interference
- Run with: `./gradlew createGplayDebugCoverageReport -Pcoverage=true`
- Run specific test class: `./gradlew createGplayDebugCoverageReport -Pcoverage=true -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.ClassName>`
- Run one test method: `./gradlew createGplayDebugCoverageReport -Pcoverage=true -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.ClassName>#methodName`

### Screenshot Tests (Shot)
- Enabled via `SHOT_TEST=true` environment variable
- Use: `scripts/androidScreenshotTest` to check, `scripts/updateScreenshots.sh` to regenerate
- CI renders shadows differently; 0.5% tolerance is configured

## Code Quality Tools

All code is validated with the following tools. Fix findings in modified files:
- **lint** - Android linting (configured in `app/lint.xml`)
- **spotbugsGplayDebug** - Bug detection for Gplay variant
- **detekt** - Kotlin code analysis (configured in `app/detekt.yml`)
- **spotlessKotlinCheck** - Code formatting with ktlint

Run all checks with: `./gradlew check`

## Fork Contribution Policy

This repository is maintained as a personal fork for local product fixes and may intentionally diverge from upstream
Nextcloud Android contribution policy.

Agents may implement, commit, and push changes when the repository owner asks for that workflow. Keep commits focused,
avoid unrelated refactors, and prefer conventional commit messages for readability.

When changing dependencies, verify artifact names and versions against real package registries before adding them.
Do not add hallucinated or unverified dependencies.

Do not open upstream issues, submit upstream pull requests, post upstream review comments, or send security reports
autonomously. If a change is intended for upstream later, the repository owner should decide the upstream submission
process separately.

## Commit and Pull Request Guidelines

### Commits

- Commit messages should follow the [Conventional Commits v1.0.0 specification](https://www.conventionalcommits.org/en/v1.0.0/#specification) where practical, e.g. `feat(chat): add voice message playback`, `fix(call): handle MCU disconnect gracefully`.
- Do not add `Signed-off-by` or `Assisted-by` trailers unless the repository owner explicitly asks for them.

### Pull Requests

- Pull requests are optional in this fork. When requested, include a short summary of what changed and why.

## Code Style

- Do not exceed 300 lines of code per file.
- Line length: **120 characters**
- Standard Android Studio formatter with EditorConfig.
- Kotlin preferred for new code; legacy Java still present.
- Do not use decorative section-divider comments of any kind (e.g. `// ── Title ───`, `// ------`, `// ======`).
- Every new file must end with exactly one empty trailing line (no more, no less).
- Do not add comments, documentation for every function you created instead make it self explanatory as much as possible.
- Create models, states in different files instead of doing it one single file.
- Do not use magic numbers.
- Apply fail fast principle instead of using nested if-else statements.
- Do not use multiple boolean flags to determine states instead use enums or sealed classes.
- Use modern Java for Java classes. Optionals, virtual threads, records, streams if necessary.
- Avoid hardcoded strings, colors, dimensions. Use resources.
- Run lint, spotbugsGplayDebug, detekt, spotlessKotlinCheck and fix findings inside the files that have been changed.
