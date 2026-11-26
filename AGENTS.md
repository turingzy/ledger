# AGENTS.md

Guidance for coding agents working in this repository.

## Project Overview

Ledger is a local-first Android finance app built with Kotlin, Jetpack Compose, Room, Preferences DataStore, and Hilt. `:app` is the only application module. Build conventions live in `build-logic/` and dependency versions in `gradle/libs.versions.toml`. Gradle uses the daemon JVM configured in `gradle/gradle-daemon-jvm.properties`. The Android application convention sets Java source and target compatibility to 17.

## Local Validation

Run these checks for app code changes.

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug --offline
```

For package moves or changes that affect instrumentation test sources, also compile those sources. After package moves, search all source sets for stale imports and package declarations.

```bash
./gradlew :app:compileDebugAndroidTestKotlin --offline
```

Validation is local only. Do not install apps, run device tests, or operate phones and emulators unless explicitly requested in the current task.

## Repository Structure

Main Kotlin sources live under `app/src/main/kotlin/cn/turingzy/ledger/`.

- `data/database/` holds Room entities, DAOs, migrations, mappers, and seed data.
- `data/repository/` groups repositories by domain. Currency selection, rates, and valuation live in `currency/`. App preferences and card appearance live in `settings/`.
- `data/transfer/` holds bill file parsing, import planning, duplicate detection, and backup validation.
- `model/` holds business models and calculations shared across layers. Keep it free of Android UI dependencies.
- `di/` owns Hilt bindings, database setup, and dispatcher qualifiers. `navigation/` owns routes and destination coordination.
- `ui/feature/` contains bill, analytics, wallet, and settings screens with their ViewModels and feature-local components. Analytics calculations and the consumption calendar live under `ui/feature/analytics/calculation/` and `ui/feature/analytics/calendar/`.
- `ui/component/` contains shared UI in `base/`, `biz/`, `card/`, `input/`, `navigation/`, and `picker/`. Shared styling and interaction behavior live in `ui/theme/` and `ui/interaction/`.

Mirror production packages in `app/src/test/` and `app/src/androidTest/`. Prefer cohesive, singular directory names and colocate Compose previews with their components.

## Development Conventions

- Store and calculate money as `Long` cents. Use `BigDecimal` for rates and rounding, and reuse `convertCents` for currency conversion.
- Preserve original transaction amounts and historical currency snapshots. Reuse `MoneyValuation` for valuation and `primaryAmount` and `primaryBalance` for primary-currency totals. Keep the source and destination amounts of cross-currency transfers distinct.
- Keep transaction writes and related account balance updates atomic. Preserve import duplicate detection and backup validation. When changing the Room schema, add a migration in `LedgerMigrations.kt` and register it in `DatabaseModule.kt`.
- Avoid `!!`, empty catches, and swallowing coroutine cancellation.
- Expose read-only state flows and inject coroutine dispatchers.
- Keep DAOs and Room entities out of UI/ViewModels. Coordinate feature navigation through `navigation/`.
- Reuse shared scaffolds, overlays, form controls, business amount displays, and theme tokens. When adding user-facing Compose strings, update the default and English resources under `app/src/main/res/`.
- Use official Material Symbols Rounded (400/0/24) through `ui/component/MaterialSymbols.kt`. Add symbols to `scripts/sync_material_symbols.py` and regenerate the Kotlin file. Leave generated vector paths to the script.
- Give classes and functions concise, clear comments that explain their purpose or non-obvious constraints. Use concise English commit messages.

## Change Scope

Keep changes limited to the task. Preserve existing calculations, database and backup compatibility, persisted preferences, and unrelated UI layout, appearance, and interactions.
