# AGENTS.md

Guidance for coding agents working in this repository.

## Project Overview

Ledger is a local-first Android finance app built with Kotlin, Jetpack Compose, Room, DataStore, and Hilt. It uses a single app module with shared convention plugins in `build-logic/`.

## Commands

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug --offline
./gradlew :app:compileDebugAndroidTestKotlin --offline
```

Validation is local only. Do not install apps, run device tests, or operate phones and emulators unless explicitly requested in the current task.

## Repository Structure

Source packages live under `app/src/main/kotlin/cn/turingzy/ledger/`:

- `data/database/`: Room entities, DAOs, migrations, mappers, and seed data.
- `data/repository/`: Repository interfaces and implementations grouped by business domain.
- `data/transfer/`: Bill file parsing, import planning, and backup validation.
- `model/`: Business models grouped by domain; keep Android UI dependencies out.
- `di/`: Hilt bindings and coroutine dispatcher qualifiers.
- `navigation/`: Application routes and navigation coordination.
- `ui/feature/`: Bill, analytics, wallet, and settings screens with feature-local components.
- `ui/component/`: Shared base, business, picker, card, input, and navigation components.
- `ui/theme/` and `ui/interaction/`: Shared styling and interaction behavior.

Keep test packages aligned with the code they exercise. Prefer cohesive, singular directory names. Place preview files in the same directory as the UI components they preview; avoid cross-feature preview collections.

## Development Conventions

- Store and calculate money as `Long` cents; use `BigDecimal` for rates and rounding.
- Avoid `!!`, empty catches, and swallowing coroutine cancellation.
- Expose read-only state flows and inject coroutine dispatchers.
- Keep DAOs and Room entities out of UI/ViewModels. Coordinate feature navigation through `navigation/`.
- Reuse shared components and theme tokens. Preserve unrelated layout and interactions.
- Use official Material Symbols Rounded (400/0/24) through `ui/component/MaterialSymbols.kt`. Add symbols through `scripts/sync_material_symbols.py`; do not edit generated vector paths.
- Keep comments focused on non-obvious reasons. Use concise English commit messages.

## Validation and Change Scope

Run the relevant local checks after changes. For package moves, also compile Android test sources and check for stale imports. Keep calculation behavior, database schema, persisted formats, and UI appearance unchanged unless the task explicitly requires changes.
