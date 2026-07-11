# KidsGames Android App

A minimal Android Kotlin app scaffold for the children's coloring and tracing experience.

## Open in Android Studio

1. Open Android Studio.
2. Select `Open` and choose the project folder: `/Users/juan.luna/Documents/Projects/kid-games`.
3. Let Android Studio sync Gradle and download dependencies.

## Build APK

From the terminal in the project root:

```bash
./gradlew assembleDebug
```

The generated APK will be available at:

- `app/build/outputs/apk/debug/app-debug.apk`

## App Features

The app includes three interactive modules designed for children:

### 1. ¡A Colorear! (Coloring Module)
- Paint over built-in outline templates: Bob, Pocoyo, Tra 1 and Tra 2 (imported PNGs, each originally a 9-drawing grid on a navy background; background processed to transparent so only the black/gray lines remain and the grid divisions are removed), plus Estrella, Corazón, Casa, Flor
- **Smart trace layer:** strokes always render *under* the black outline, so the result stays clean
- Non-square templates (Bob, Pocoyo, Tra 1, Tra 2) keep their original aspect ratio instead of being stretched into a square
- Giant color-pencil-shaped palette (rounded rectangles) in a horizontal row below the canvas, adapted for children's fingers (Rojo, Naranja, Amarillo, Verde, Azul, Morado, Rosa, Café, Blanco, Negro), plus a "Clear All" button (same pencil style, white, trash-can icon) that resets the canvas to blank
- Thin brush strokes (16px) for more precise coloring inside outlines
- Top toolbar: zoom in/out, reset, save, and an anti-mistake **lock** (hold 3s to unlock)
- **Pan while zoomed:** tap the triangular arrow buttons (up/down/left/right) overlaid on the canvas edges to reveal that side of the image (e.g. the left arrow shows the left part of the drawing); each arrow only shows while there's room left to move in that direction, and disappears at the edge of the canvas frame
- Saved drawings export as JPEG (quality 80%) and appear in the **"Mis Creaciones"** virtual-fridge screen

### 2. Une los Puntos (Tracing Module)
- Practice writing numbers (1-20) and letters (A-Z, a-z)
- Guided point-by-point tracing with visual feedback
- Once tracing is complete, press "Dibujar" (Draw) to unlock coloring mode
- Paint the traced character with available colors

### 3. Pizarra Libre (Free Drawing Canvas) - NEW FLOW
- **Direct drawing experience:** No intermediate steps
- Upon selection from the main menu, a blank canvas appears immediately
- Giant pencil-shaped color palette (rounded rectangles, `pencil_button_bg`) in a horizontal scrollable row **below the canvas** (Rojo, Azul, Amarillo, Verde, Morado, Café, Rosa, Negro)
- Child can draw freely without tracing requirements
- Intuitive tools at the end of the palette: a white circle with a black border (`white_circle_border_bg`) that paints in white, and an undo button with the same white-circle style and a black trash-can icon
- "Ir al Inicio" (Go to Home) button to return to the main menu

## Design Notes

- `local.properties` points to your local Android SDK installation.
- The app uses a child-friendly interface with large buttons and intuitive controls.
- The tracing (Une los Puntos) and free-drawing (Pizarra Libre) modules share the same `colorToolsPanel` in `PlaygroundActivity`/`activity_playground.xml`, so palette/tooling changes apply to both.
- All modules support drawing with multiple colors and an undo tool; there is no separate eraser mode anymore — painting white achieves the same result.

## Known Issues & Troubleshooting

### Resolved: ¡A Colorear! (Coloring Module) - Not Implemented Per Spec

**Status:** ✅ Implemented (v1.3) — verified on emulator

**Symptom:** Selecting "Colorear" from the menu did not open a coloring canvas. `ColoringActivity` was a placeholder that only forwarded to `PlaygroundActivity` (the letter/number tracing flow), so none of the spec's coloring features existed.

**Fix (v1.3):** Built the module properly:
- New `ColoringView` custom view renders the child's strokes into a fill bitmap and draws the black template outline on top every frame — the spec's **smart trace layer**.
- New `activity_coloring.xml` with a top toolbar (zoom +/−, reset, save, lock), a template picker, a central canvas, and a right-side palette of giant circular color buttons + broom eraser.
- `ColoringActivity` wires the palette/templates, the 3-second hold-to-unlock **lock** that disables all editing, and JPEG export (quality 80%).
- New `CreationsActivity` (`activity_creations.xml`) is the **"Mis Creaciones"** virtual-fridge gallery, opened automatically after saving.
- Four built-in outline templates added as vector drawables (star, heart, house, flower).

**Verification:** Built + installed on emulator. Confirmed: strokes render under the outline (red/yellow scribbles stayed beneath the black star lines), color/eraser selection works, Save opens "Mis Creaciones" with the framed image, and activating the lock blocks drawing until held 3s.

**Pending (documented in `kids-games.md`):** loading parent-provided templates from `/DibujosParaColorear/`, and honoring the Internal/MicroSD storage selector (currently always app-internal storage).

### Resolved: Pizarra Libre (Free Drawing) - Drawing Not Responding

**Status:** ✅ Fixed (v1.2) — verified on emulator

**Symptom:** When entering "Pizarra Libre" and selecting a color, touching the canvas produced no drawing.

**Root Cause:** An initialization-timing bug. `PlaygroundActivity` called `setFreeMode(true)` and `enableColoringMode()` inside `onCreate`, before the `TraceView` had been measured (`width`/`height` were still 0). As a result:
- `setFreeMode(true)` skipped preparing the fill bitmap and never set `completed`, because the view had no size yet.
- `enableColoringMode()` returned early at its `if (!completed) return` guard, so `coloringEnabled` stayed `false`.
- When `onSizeChanged` later created the bitmap, nothing re-enabled coloring, so `onTouchEvent` never satisfied `completed && coloringEnabled` and touches were ignored.

**Fix (v1.2):** In `TraceView.kt`:
- `onSizeChanged` (free mode) and `setFreeMode` now set `coloringEnabled = true` once the canvas has real dimensions and the fill bitmap is ready.
- `enableColoringMode()` no longer bails out in free mode when the view is not yet measured.
- `ACTION_DOWN` now paints a dot on a simple tap (not just on drag), so a single touch also leaves a mark.

**Verification:** Built and installed on an emulator (`adb`/`gradlew installDebug`), opened Menú → Pizarra Libre, selected Rojo/Verde/Azul in turn and confirmed each color draws continuously along the swipe path.

Drawings can be saved to internal storage or external SD card.

