---
name: sugerir-mejoras
description: Crea una branch nueva desde main, analiza la app KidsGames y genera un reporte de sugerencias y mejoras (bugs, UX infantil, rendimiento, código, contenido). Úsala cuando el usuario pida /sugerir-mejoras o "dame sugerencias de mejora".
argument-hint: "[enfoque opcional, ej. 'coloreado', 'rendimiento', 'accesibilidad']"
disable-model-invocation: true
---

# Sugerir mejoras para KidsGames

Cada ejecución crea **una branch nueva** y produce un reporte de sugerencias priorizadas.
No implementes nada sin aprobación del usuario: esta skill solo analiza y propone.

Enfoque solicitado (puede venir vacío = revisión general): `$ARGUMENTS`

## Paso 1 — Crear la branch

1. Revisa `git status --porcelain`. Ignora archivos `.DS_Store` sin rastrear.
   Si hay cambios en archivos rastreados sin commitear, **detente y pregunta** al usuario
   si quiere hacer commit, stash o cancelar. No los descartes.
2. Cámbiate a `main`. Si existe un remoto (`git remote` no vacío), ejecuta `git pull --ff-only`;
   si falla, avisa y continúa desde el `main` local.
3. Crea la branch con fecha y hora para que siempre sea única:
   ```bash
   git checkout -b "mejoras/$(date +%Y-%m-%d-%H%M)"
   ```
   Si se dio un enfoque, añádelo como sufijo en kebab-case (ej. `mejoras/2026-11-19-1530-coloreado`).

## Paso 2 — Reunir contexto

- Lee `CLAUDE.md`, `status-tasks.md`, `kids-games.md` y la sección "Known Issues" de `README.md`.
- Lee los reportes anteriores en `docs/mejoras/` (si existen) para **no repetir** sugerencias
  ya hechas; si una sugerencia anterior sigue sin resolverse, menciónala como "pendiente de reporte previo".
- Revisa `git log --oneline -20` para ver qué cambió recientemente.
- Lee el código relevante en `app/src/main/java/com/example/kidsgames/` y los layouts en
  `app/src/main/res/layout/`. Si hay enfoque, prioriza los archivos de ese módulo.

## Paso 3 — Analizar

Busca oportunidades en estas categorías (omite las que no tengan hallazgos reales):

1. **Bugs y riesgos** — crashes posibles, fugas de memoria (bitmaps grandes, TTS sin `shutdown`),
   estado perdido al rotar o al pasar a segundo plano, condiciones de layout (ver gotcha de `onSizeChanged`).
2. **Experiencia para niños pequeños** — botones grandes, feedback visual/sonoro, cero texto obligatorio,
   prevención de salidas accidentales, frustración en el trazado (tolerancia del imán).
3. **Accesibilidad** — `contentDescription`, contraste, tamaños táctiles ≥ 48dp.
4. **Rendimiento** — trabajo pesado en `onDraw`, asignaciones por frame, decodificación de imágenes.
5. **Código y arquitectura** — código legado (`CanvasActivity`, `TracingActivity`), duplicación,
   `TraceView` demasiado grande, strings hardcodeados fuera de `strings.xml`.
6. **Pendientes del spec** — ítems pendientes/parciales de `status-tasks.md`.
7. **Ideas nuevas** — contenido o mecánicas nuevas acordes al público (plantillas, sonidos, recompensas).
8. **Calidad del proyecto** — ausencia de tests, configuración de build, privacidad/seguridad infantil.

Cada sugerencia debe basarse en algo que **viste en el código o la documentación**, con referencia
`archivo:línea` cuando aplique. No inventes problemas genéricos.

## Paso 4 — Escribir el reporte

Guarda el reporte en `docs/mejoras/<nombre-de-la-branch-sin-prefijo>.md` (ej. `docs/mejoras/2026-11-19-1530.md`)
con este formato, en español:

```markdown
# Sugerencias de mejora — <fecha>

Branch: `mejoras/...` · Enfoque: <enfoque o "general">

## Resumen
<2–3 frases con el estado general y las 3 prioridades principales>

## Sugerencias

### 1. <Título corto>
- **Categoría:** <categoría>
- **Impacto:** Alto | Medio | Bajo
- **Esfuerzo:** Alto | Medio | Bajo
- **Dónde:** `ruta/archivo.kt:123`
- **Problema:** <qué pasa hoy y por qué importa>
- **Propuesta:** <qué cambiar, concreto>

(ordena de mayor a menor prioridad: impacto alto + esfuerzo bajo primero)

## Pendientes de reportes anteriores
<lista, o "Ninguno">
```

Haz commit del reporte en la branch nueva:
```bash
git add docs/mejoras/
git commit -m "docs: sugerencias de mejora <fecha>"
```
(No hagas push salvo que el usuario lo pida.)

## Paso 5 — Presentar al usuario

Muestra en el chat:
- El nombre de la branch creada y la ruta del reporte.
- Las **5 sugerencias principales** en una tabla breve (título, impacto, esfuerzo).
- Pregunta cuáles quiere implementar en esta branch.

Si el usuario elige implementar alguna, hazlo en esta misma branch, verifica con
`./gradlew assembleDebug`, y actualiza `status-tasks.md` / `kids-games.md` / `README.md` si cambia el comportamiento.
