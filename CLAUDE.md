# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

KidsGames: a single-module Android app (Kotlin, Views/XML layouts, no Compose) for young children with three modules: **¡A Colorear!** (coloring templates), **Une los Puntos** (dot-to-dot tracing of numbers/letters), and **Pizarra Libre** (free drawing). UI text, docs, and commit messages are in Spanish.

## Commands

```bash
./gradlew assembleDebug              # build debug APK
./gradlew installDebug               # build + install on connected device/emulator
./gradlew clean assembleDebug
```

- The APK filename is overridden in `app/build.gradle.kts` to `kids-games<version>.apk` (e.g. `app/build/outputs/apk/debug/kids-games1.0.0.apk`), not the default `app-debug.apk`.
- Toolchain: AGP 8.2.1, Kotlin 1.9.10, JDK 17, compileSdk/targetSdk 34, minSdk 24.
- There are no unit or instrumented tests in the repo (test dependencies are declared but `app/src/test` / `app/src/androidTest` don't exist). Changes are verified by building and exercising the app on an emulator.

## Architecture

All code lives in `app/src/main/java/com/example/kidsgames/`.

- **`MainActivity`** shows a RecyclerView of `ModuleItem`s and launches each module's activity with a `moduleName` intent extra.
- **Une los Puntos and Pizarra Libre share one activity**: both launch `PlaygroundActivity`, which branches on `moduleName == "Pizarra Libre"`. Both also share `TraceView` and the single `colorToolsPanel` in `activity_playground.xml`, so palette/tool changes affect both modules.
  - `TraceView` (~1000 lines) handles glyph-to-dot-path generation, magnet tolerance, blinking active point, TTS feedback (Spanish locale), confetti, the "Dibujar" reveal, and coloring clipped to the glyph region. In free mode (`setFreeMode(true)`) it skips tracing and draws directly.
  - Gotcha (previously caused a bug): `TraceView` state such as `coloringEnabled` depends on the view being measured; anything set in `onCreate` before layout must be re-applied in `onSizeChanged`.
- **`ColoringActivity` + `ColoringView`** implement the coloring module. `ColoringView` keeps the child's strokes in a `fillBitmap` and draws the template drawable **on top** each frame ("smart trace layer"), so strokes always appear under the outline. It also owns zoom (1×–3×), arrow-button panning with clamping (`canPan*`/`clampTranslation`), lock state, and `exportBitmap()`.
- **`CreationsActivity`** ("Mis Creaciones" fridge gallery) lists JPEGs saved to `filesDir/DibujosParaColorear`.
- `CanvasActivity` and `TracingActivity` are legacy intermediate screens; the main menu no longer routes through them.

## Adding coloring templates

See `AGREGAR_PLANTILLAS.md` for the full procedure. Key points:
- Templates are registered in the `templates` list in `ColoringActivity.kt` as `"Nombre" to R.drawable.template_<nombre>`; the first entry is the default.
- Raster templates go in `res/drawable-nodpi/template_<nombre>.png` and **must have a transparent background** (RGBA). An opaque white background covers the child's strokes because the template is drawn over `fillBitmap`. The doc includes a PIL script that turns near-white (>240) pixels transparent.
- Vector templates (`template_star/heart/house/flower.xml`) live in `res/drawable/`.
- `ColoringView.templateBounds()` preserves aspect ratio for non-square templates.

## Project docs

- `kids-games.md`: the design spec (Spanish), with "implementation notes" sections recording how and why the implementation differs from the original plan. Keep it in sync when behavior changes.
- `status-tasks.md`: a checklist of spec items marked implemented, pending, or partial. Known pending items: loading parent-provided templates from `/DibujosParaColorear/`, and honoring the internal/MicroSD storage selector (saves currently always go to app-internal storage).
- `README.md`: feature overview plus a "Known Issues & Troubleshooting" log of resolved bugs.
