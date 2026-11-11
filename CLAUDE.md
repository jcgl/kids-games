# CLAUDE.md — Contexto del proyecto Kids Games

> Este archivo lo lee Claude **automáticamente** al trabajar en el repo (CLI y GitHub Action).
> Su objetivo es que cualquier cambio **reutilice** lo que ya existe y **no duplique** código,
> componentes, pantallas, strings, colores ni drawables.

## Qué es

App Android nativa en **Kotlin** para niños, con tres módulos de dibujo. UI 100% en **español**.

- `applicationId` / `namespace`: `com.example.kidsgames`
- `minSdk` 24, `targetSdk`/`compileSdk` 34, Java/Kotlin **17**
- Dependencias: AndroidX core-ktx, appcompat, **Material Components**, ConstraintLayout. No hay Jetpack Compose: todo es **Vistas + XML** (`findViewById`, `RecyclerView`, custom `View`).

### Compilar (referencia — la Action NO compila; las pruebas las hace el usuario)
```bash
./gradlew assembleDebug      # genera app/build/outputs/apk/debug/kids-games1.0.0.apk
./gradlew installDebug       # instala en emulador/dispositivo
```

## Arquitectura — mapa de archivos

Código en `app/src/main/java/com/example/kidsgames/`:

| Archivo | Responsabilidad |
|---|---|
| `MainActivity.kt` | Menú principal. Define la **lista de módulos** (`ModuleItem`) y lanza cada Activity. Aquí se registra cualquier módulo nuevo. |
| `ModuleItem` (data class en `MainActivity.kt`) | Modelo de una tarjeta del menú: `title, subtitle, icon(emoji), colorRes, activityClass`. |
| `ModuleAdapter.kt` | `RecyclerView.Adapter` que pinta las tarjetas usando `module_card.xml`. **Reutilizar**, no crear otro adapter para listas de tarjetas. |
| `ColoringActivity.kt` | Módulo **"Colorear"**. Dueño de la lista `templates` (plantillas), paleta de colores, toolbar (zoom/reset/save/lock) y exportación a JPEG. |
| `ColoringView.kt` | Custom `View` del coloreo. Dibuja `fillBitmap` (trazos del niño) y **encima** `templateDrawable` → "capa inteligente". |
| `PlaygroundActivity.kt` | Módulos **"Une los Puntos"** (trazado) y **"Pizarra Libre"** (dibujo libre). Ambos comparten el mismo `colorToolsPanel`. |
| `TraceView.kt` | Custom `View` grande (~1000 líneas) para trazado guiado y modo libre/coloreo. Núcleo del dibujo punto-a-punto. |
| `CreationsActivity.kt` | Galería "Mis Creaciones" (refrigerador virtual) con las imágenes guardadas. |
| `TracingActivity.kt` / `CanvasActivity.kt` | Activities auxiliares/legadas. Revisar antes de ampliar para no crear flujos paralelos. |

Recursos en `app/src/main/res/`:
- `layout/` — un XML por pantalla (`activity_*.xml`) + `module_card.xml`.
- `values/` — `strings.xml`, `colors.xml`, `themes.xml`.
- `drawable/` — iconos y fondos vectoriales (`ic_*.xml`, `*_bg.xml`) y plantillas vectoriales (`template_star/heart/house/flower.xml`).
- `drawable-nodpi/` — plantillas PNG de alta resolución (`template_*.png`) que **no** deben escalarse.

Activities registradas en `app/src/main/AndroidManifest.xml` — toda Activity nueva debe declararse ahí.

## Reglas para NO duplicar (leer antes de implementar)

1. **Busca primero.** Antes de crear una clase/pantalla/función, busca si ya existe algo equivalente (grep por nombre y por responsabilidad). Reutiliza o extiende.
2. **Tarjetas de menú** → añade un `ModuleItem` en `MainActivity.kt` y, si hace falta, un color en `colors.xml`. No crees otro adapter ni otro layout de tarjeta: usa `ModuleAdapter` + `module_card.xml`.
3. **Paleta de colores y herramientas de dibujo** → "Une los Puntos" y "Pizarra Libre" comparten `colorToolsPanel` en `PlaygroundActivity`/`activity_playground.xml`. Un cambio ahí aplica a ambos; no dupliques la paleta.
4. **Coloreo** → la lógica vive en `ColoringView`; el cableado (plantillas, paleta, toolbar, guardado) en `ColoringActivity`. No reimplementes render de bitmap/trazos.
5. **Plantillas de colorear** → sigue **`AGREGAR_PLANTILLAS.md`** al pie de la letra: PNG con **fondo transparente** (script PIL incluido) en `drawable-nodpi/template_<nombre>.png`, y registra la entrada en la lista `templates` de `ColoringActivity.kt`. Vectoriales van como `template_<nombre>.xml` en `drawable/`.
6. **No hardcodear.** Textos → `strings.xml`; colores → `colors.xml`; iconos/fondos → drawables existentes (`ic_*`, `*_bg`). Reutiliza drawables antes de crear uno nuevo.
7. **Sin eraser separado:** pintar de blanco es el borrador. No reintroduzcas un modo goma.
8. **Idioma:** todo texto visible al usuario en **español**.

## Convenciones de nombres

- Archivos de plantilla: `template_<nombre>.png|xml` (minúsculas, guion bajo). Recurso: `R.drawable.template_<nombre>`. Entrada en lista: `"<Nombre>" to R.drawable.template_<nombre>`.
- Layouts: `activity_<pantalla>.xml`. Drawables: iconos `ic_<nombre>.xml`, fondos `<nombre>_bg.xml`.
- Clases Kotlin en PascalCase; una Activity por pantalla, declarada en el manifest.

## Documentación

Hay docs en la raíz que se mantienen al día (en español): `README.md` (features y troubleshooting),
`AGREGAR_PLANTILLAS.md` (guía de plantillas), `SOLUCION_ESQUELETO.md`, `kids-games.md`, `status-tasks.md`.
Al resolver un issue, **documenta la solución** en el mismo estilo: qué se cambió, por qué, archivos tocados
y cómo verificar. Si el cambio afecta features del `README.md`, actualízalo.

## Flujo esperado al resolver un issue (GitHub Action)

El usuario **prueba manualmente**; si algo falta, abre otro issue. Por eso, al resolver un issue:
- Lee este archivo y el código existente relacionado **antes** de escribir nada.
- Reutiliza componentes según las reglas de arriba.
- Rama `claude/issue-<número>`, PR con `Closes #<número>`.
- **No** ejecutes build ni pruebas; entrega código + documentación de la solución.
