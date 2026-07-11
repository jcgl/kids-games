# ✅ Estado de Implementación — KidsGames

Checklist de verificación contra `kids-games.md` (especificación de diseño).
Marcado: `[x]` implementado y verificado en código · `[ ]` pendiente · `[~]` parcial / con observación.

Última revisión: 2026-07-10

---

## 📱 Módulo 1: ¡A Colorear!  (`ColoringActivity` + `ColoringView`)

- [x] Lienzo central que detecta gestos táctiles para pintar (`ColoringView.onTouchEvent`)
- [x] Paleta horizontal debajo del lienzo con botones gigantes en forma de lápiz de color (rectángulos redondeados 40×84dp) (`buildColorPalette`, `pencilBg`)
- [x] Colores: Rojo, Naranja, Amarillo, Verde, Azul, Morado, Rosa, Café, Blanco, Negro
- [x] Grosor de pincel delgado (16px) (`ColoringView.linePaint`)
- [x] Botón "Borrar todo" (mismo estilo de lápiz, en blanco, + icono de bote de basura) al final de la paleta; reinicia el lienzo (`reset()`) en vez de un borrador puntual, ya redundante con el color Blanco
- [x] Barra superior: Zoom In / Zoom Out (escala 1×–3×)
- [x] Mover el lienzo ampliado con 4 flechas triangulares (arriba/abajo/izquierda/derecha) superpuestas en los bordes del lienzo, más intuitivas para niños que un gesto de arrastre (`ColoringView.panUp/Down/Left/Right`, `ColoringActivity.setupPanButtons`); cada flecha revela ese lado del dibujo (presionar izquierda muestra la parte izquierda de la imagen)
- [x] Cada flecha se oculta automáticamente al llegar al límite de esa dirección o si no hay zoom (`canPanUp/Down/Left/Right`, `clampTranslation`)
- [x] Botón de Guardar
- [x] Botón de Reset
- [x] Candado anti-errores: bloquea dibujo, zoom, movimiento, reset, guardar y selector de plantillas
- [x] Desbloqueo manteniendo presionado 3 segundos (icono candado abierto/cerrado)
- [x] Capa de Trazo Inteligente: trazos por DEBAJO del contorno negro (`onDraw`)
- [x] Plantillas integradas: Bob, Pocoyo, Tra 1, Tra 2 (PNG con fondo transparente, sin la cuadrícula original), Estrella, Corazón, Casa, Flor (vectoriales)
- [x] Ajuste de aspecto de plantillas no cuadradas sin deformar (`ColoringView.templateBounds`)
- [x] Efecto "Nevera Virtual": guarda y abre "Mis Creaciones" (`CreationsActivity`)
- [x] Exportación JPEG calidad 80%
- [x] Pantalla "Mis Creaciones" con marcos tipo imán sobre fondo de refrigerador
- [ ] Carga de plantillas externas del padre desde `/DibujosParaColorear/` (pendiente, documentado)
- [ ] Respetar selector Interna/MicroSD al guardar (hoy siempre almacenamiento interno de la app)

---

## 🅰️🔢 Módulo 2: Une los Puntos  (`PlaygroundActivity` + `TraceView`)

### Flujo del juego
- [x] Selección de categoría: Números (1–20), Mayúsculas (A–Z), Minúsculas (a–z)
- [x] Catálogo en cuadrícula para elegir letra/número
- [x] Pantalla de trazado con el elemento gigante compuesto por puntos grises

### Mecánicas y UX
- [x] Modo Imán (tolerancia): el trazo se magnetiza al punto cercano (`magnetRadius`)
- [x] Punto Activo parpadeante en color llamativo (`startBlink` + `activePaint`)
- [x] Refuerzo auditivo: TTS dice la letra/número al completar ("¡Cinco!", "¡Letra B!")
- [x] Botón Dibujar: revela contorno y habilita coloreo solo al presionarlo
- [x] Panel de trazo arriba, paleta/herramientas en fila horizontal debajo del lienzo (`colorToolsPanel`, antes vertical a la derecha)
- [x] Botones de color en forma de rectángulo redondeado (`pencil_button_bg`), con el color que representan + botón Negro y Rosa adicionales
- [x] Botón de pintar en blanco: círculo blanco con borde negro (`btnColorWhite`, `white_circle_border_bg`), reemplaza al antiguo borrador con icono de escoba
- [x] Botón Undo: círculo blanco con borde negro e icono de cesto de basura en negro (`btnRevertColoring`)
- [x] Transición limpia: al colorear se ocultan puntos/rastro de trazado
- [x] Navegación: fila separada con "Volver a selección" e "Inicio"
- [x] "Ir al inicio" al final, debajo de la cuadrícula, centrado (selectionPanel)
- [x] Botones del módulo sin mayúsculas forzadas (etiquetas en formato natural)
- [x] Sin icono extra de navegación (solo "Ir al inicio")
- [~] Animación de confeti al completar al 100%
      ⚠️ Código existe (`ConfettiParticle`, `updateConfetti`, `drawConfetti`) pero
      NUNCA se invoca: `drawConfetti` no se llama en `onDraw` y `confettiParticles`
      nunca se llena. **Confeti NO se muestra en pantalla — pendiente de conectar.**
- [~] Flechas guía que indican la dirección del trazo
      ⚠️ El orden secuencial de puntos sí guía la dirección, pero no se dibujan
      flechas explícitas como describe la especificación.
- [x] Esqueletos de trazo por carácter
      Definidos para dígitos 0–9, mayúsculas A–Z y minúsculas a–z (`skeletonFor`),
      todos sobre la línea central del trazo. Las minúsculas se normalizan al
      recuadro real del glifo (con ascendentes/descendentes). Solo caracteres fuera
      de ese conjunto caen al respaldo del contorno.

### Función: Subir imágenes con asociación visual
- [ ] Subida de foto asociada a una letra (ej. Perro → P) mostrada como referencia
- [ ] Subida de foto con N objetos asociada al número N como referencia

---

## ✏️🌈 Módulo 3: Pizarra Libre  (`PlaygroundActivity` modo "Pizarra Libre" + `TraceView.setFreeMode`)

- [x] Acceso directo a pizarra en blanco, sin pasos intermedios
- [x] Flujo lineal: Menú → Pizarra Libre → lienzo en blanco
- [x] Dibujo activo desde el primer toque (coloreo auto-habilitado en `onSizeChanged`)
- [x] Un toque simple también deja marca (ACTION_DOWN pinta punto)
- [x] Tablero blanco que ocupa el ancho de la pantalla central
- [x] Paleta de colores gigantes en fila horizontal debajo del lienzo (antes vertical a la derecha), con botones en forma de rectángulo/lápiz e incluyendo Rosa
- [x] Botón de pintar en blanco: círculo blanco con borde negro (reemplaza a la goma de borrar con icono de escoba; se eliminó el modo borrador de `TraceView`)
- [x] Botón Deshacer: círculo blanco con borde negro e icono de cesto de basura en negro
- [x] Botón Ir al Inicio
- [ ] Selector de Grosor inferior (Pequeño / Mediano / Grande) — NO implementado
- [ ] Guardar en la Nevera los dibujos libres con fondo blanco — NO hay botón Guardar
      en el flujo de Pizarra Libre (solo el módulo Colorear guarda)
- [~] Subtítulo del módulo en `MainActivity` dice "Mismo flujo de trazado y coloreado
      con botón Dibujar", lo que contradice el flujo directo real. (Texto desactualizado.)

---

## 💾 Módulo 4: Gestión de Almacenamiento y Tarjeta SD (Menú de Padres)

- [ ] Panel de configuración con bloqueo parental (resolver una suma simple)
- [ ] Selector de ubicación: Memoria Interna vs Tarjeta MicroSD Externa
- [ ] Detección de MicroSD (`Context.getExternalFilesDirs()`)
- [ ] Carpeta `/DibujosParaColorear/` dinámica en la raíz seleccionada
- [~] Optimización de espacio: JPEG calidad 80%
      (✅ implementado solo en el guardado del módulo Colorear)

---

## 🛠️ Roadmap Técnico

- [ ] **Fase 1:** Rutas/almacenamiento + menú parental (MicroSD) — pendiente
- [x] **Fase 2:** Motor del lienzo (pintar con el dedo, color y grosor de pincel)
      ⚠️ El cambio de grosor de pincel no está expuesto en la UI (ver Pizarra Libre)
- [x] **Fase 3:** Interfaz infantil, botones gigantes, Reset, Borrador, Candado
- [x] **Fase 4:** Módulo de trazado (puntos guía, orden 1–20/A–Z, audio)
      ⚠️ Esqueletos definidos para dígitos, mayúsculas y minúsculas; confeti sin conectar

---

## 🧹 Notas de limpieza (no son requisitos de la especificación)

- [ ] `TracingActivity` y `CanvasActivity` existen y están declaradas en el Manifest,
      pero quedan huérfanas: `MainActivity` abre directamente `ColoringActivity` y
      `PlaygroundActivity`, por lo que estas dos pantallas intermedias no se usan.

---

## Resumen

| Módulo | Estado |
|--------|--------|
| 1. ¡A Colorear! | ✅ Completo (pendiente: plantillas externas + MicroSD) |
| 2. Une los Puntos | 🟡 Funcional (faltan: subida de imágenes, confeti real) |
| 3. Pizarra Libre | 🟡 Funcional (faltan: selector de grosor, guardar en nevera) |
| 4. Menú de Padres / Almacenamiento | ❌ No implementado |
