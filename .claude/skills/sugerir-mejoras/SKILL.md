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
- Lee **todos** los reportes anteriores para **no repetir** sugerencias, aunque sus branches
  no se hayan mergeado a `main`:
  ```bash
  ls docs/mejoras/ 2>/dev/null                       # reportes ya mergeados
  for b in $(git branch --list 'mejoras/*' --format='%(refname:short)'); do
    git ls-tree --name-only "$b" docs/mejoras/ 2>/dev/null | sed "s|^|$b: |"
  done                                                # reportes en otras branches
  ```
  Lee cada reporte con `git show <branch>:<ruta>` (si un mismo reporte está en `main` y en su branch,
  usa la versión más reciente). Según el **Estado** de cada sugerencia previa:
  - `Rechazada` o `Implementada` → no la vuelvas a proponer ni la listes.
  - `Pendiente` o `Aprobada` → verifica en el código si ya se resolvió; si no, inclúyela en
    "Pendientes de reportes anteriores" (con la branch de origen) en vez de repetirla como nueva.
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
- **Estado:** Pendiente
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

No implementes nada hasta que el usuario lo solicite. Cuando responda:
- Marca las elegidas como `Estado: Aprobada` y el resto como `Estado: Rechazada` en el reporte, y haz commit.
  (El usuario puede mergear el reporte a `main` aunque no apruebe nada; las rechazadas quedan como registro.)
- Si responde que no implementará ninguna, marca todas como `Rechazada` y haz commit.
- Al implementar una aprobada, hazlo en esta misma branch, verifica con `./gradlew assembleDebug`,
  cámbiala a `Estado: Implementada` y actualiza `status-tasks.md` / `kids-games.md` / `README.md`
  si cambia el comportamiento.
