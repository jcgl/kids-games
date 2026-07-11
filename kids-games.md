# 🎨 Especificación de Diseño: App Móvil de Colorear y Aprendizaje (Android Kotlin App)

Este documento contiene la planeación detallada, arquitectura de funciones y diseño de experiencia de usuario (UX) para una aplicación móvil infantil orientada al entretenimiento y estimulación motriz/cognitiva.

---

## 📱 1. Módulo Principal: ¡A Colorear!

Este módulo se enfoca en la expresión libre y en el desarrollo de la motricidad fina, permitiendo a los niños pintar sobre plantillas predefinidas o importadas.

### 📐 Estructura de la Interfaz (UI)
* **Lienzo Central (Canvas):** Zona central donde se muestra el contorno del dibujo en alta resolución. Detecta gestos táctiles para pintar.
* **Paleta Inferior Horizontal:** Fila horizontal desplazable, debajo del lienzo, con botones gigantes en forma de **lápiz de color** (rectángulos redondeados, uno por cada color) para seleccionar colores, adaptada al tamaño de los dedos de un niño.
* **Barra de Herramientas Superior:** Botones alejados de la zona común de toque para evitar errores:
    * `Zoom In / Zoom Out` (Botones $+$ y $-$).
    * `Botón de Guardar` (Icono de disquete o check).
    * `Botón de Reset` (Icono de flecha de retorno).
    * `Botón de Candado (Anti-errores):` Al activarse, bloquea el zoom, el movimiento del lienzo y los botones de reset/guardar. Requiere mantener presionado por 3 segundos para desbloquear.

### 🎨 Mecánica de Pintura e Imágenes
* **Carga de Imágenes (Sin algoritmos complejos):** Las imágenes base serán exclusivamente dibujos lineales en blanco y negro (formatos PNG con fondo transparente o JPG con fondo blanco puro).
* **Capa de Trazo Inteligente:** El código renderizará los trazos del niño por *debajo* de las líneas negras del dibujo original. Así, el resultado final siempre se verá limpio y respetará el contorno del personaje.
* **Efecto "Nevera Virtual":** Al presionar **Guardar**, el dibujo se almacena localmente en el destino seleccionado (ver sección de Almacenamiento) y se muestra en una pantalla llamada "Mis Creaciones", diseñada visualmente como un refrigerador con imanes.

### 🛠️ Notas de Implementación (estado actual)
Este módulo se implementó por completo (antes era solo un acceso que redirigía al módulo de trazado). Detalles y ajustes respecto a la planeación original:
* **Pantalla:** `ColoringActivity` + vista personalizada `ColoringView`. La pantalla "Mis Creaciones" es `CreationsActivity`.
* **Plantillas integradas:** Como aún no se cargan imágenes externas, el módulo incluye 8 plantillas de contorno integradas, seleccionables desde una fila superior: **Bob**, **Pocoyo**, **Tra 1** y **Tra 2** (imágenes PNG importadas, cada una era originalmente una cuadrícula de 9 dibujos sobre fondo azul marino; se procesó el fondo a transparente dejando solo las líneas negras/grises de los dibujos, eliminando también las divisiones de la cuadrícula) más las 4 plantillas vectoriales originales (**Estrella, Corazón, Casa, Flor**). La carga de plantillas del padre desde la carpeta `/DibujosParaColorear/` (ver sección de Almacenamiento) queda pendiente para una iteración posterior; el mecanismo de "Capa de Trazo Inteligente" ya soporta cualquier contorno.
* **Ajuste de plantillas no cuadradas:** `ColoringView.templateBounds()` ajusta el contorno respetando su relación de aspecto original (en vez de forzar un cuadrado), para que las plantillas rectangulares como Bob, Pocoyo, Tra 1 o Tra 2 no se vean deformadas.
* **Capa de Trazo Inteligente:** Implementada exactamente como se planeó. Los trazos se acumulan en un bitmap de relleno y en cada repintado se dibujan **por debajo** del contorno negro de la plantilla, por lo que el resultado siempre respeta las líneas.
* **Paleta:** Botones gigantes en forma de **lápiz de color** (rectángulos redondeados de 40×84dp, `pencilBg()`) generados dinámicamente en fila **horizontal debajo del lienzo** con los colores Rojo, Naranja, Amarillo, Verde, Azul, Morado, Rosa, Café, Blanco y Negro, más un botón **"Borrar todo"** (mismo estilo de lápiz, en blanco, con ícono de bote de basura) al final. Se cambió de círculos a este estilo de lápiz para que la paleta se vea más como una caja de lápices de colores. Pintar de blanco ya cubre el borrado puntual, así que este botón reinicia el lienzo completo a su estado inicial sin colores (llama a `reset()`, igual que el botón Reset de la barra superior).
* **Grosor de pincel:** El trazo del niño usa un grosor delgado (16px, antes 36px) para permitir un coloreado más preciso dentro de los contornos; el borrador usa 28px (antes 48px).
* **Barra superior:** Zoom + / − (escala uniforme 1×–3× del lienzo), **Reset** (limpia el coloreado y restablece el zoom), **Guardar** y **Candado**.
* **Mover el lienzo ampliado:** Al hacer zoom aparecen hasta 4 flechas triangulares superpuestas en los bordes del lienzo (arriba, abajo, izquierda, derecha; `btnPanUp/Down/Left/Right`) que desplazan la imagen un paso fijo por toque (`ColoringView.panUp/Down/Left/Right`). Se eligió este control con botones en vez de un gesto de arrastre porque es más intuitivo y descubrible para niños. Cada flecha **revela ese lado del dibujo**: presionar la flecha izquierda muestra la parte izquierda de la imagen (y así con las otras tres), en vez de mover el contenido en el sentido literal de la flecha. Cada flecha solo se muestra mientras todavía haya margen para moverse en esa dirección (`canPanUp/Down/Left/Right`); el desplazamiento queda recortado (`clampTranslation()`) para que el lienzo nunca se separe del marco naranja, así que al llegar al borde la flecha correspondiente desaparece. Sin zoom (escala 1×) no hay desplazamiento posible y no se muestra ninguna flecha.
* **Candado anti-errores:** Al activarlo se bloquean el dibujo, el zoom, el reset, el guardado y el selector de plantillas. Para desbloquear se mantiene presionado el botón del candado **3 segundos** (el icono cambia de candado abierto a cerrado).
* **Guardado / Nevera Virtual:** El dibujo se exporta como **JPEG con calidad 80 %** a la carpeta `DibujosParaColorear` dentro del almacenamiento interno de la app y, tras guardar, se abre automáticamente "Mis Creaciones", que muestra las imágenes en marcos tipo foto sobre un fondo de refrigerador. *Pendiente:* respetar el selector Interna/MicroSD del Menú de Padres (hoy se usa siempre el almacenamiento interno de la app).

---

## 🅰️🔢 2. Módulo Didáctico: Une los Puntos (Trazado Gigante)

Módulo enfocado en la grafomotricidad, el conteo y la alfabetización temprana. La letra o el número en sí mismo es el lienzo principal en un formato gigante.

### 🧭 Flujo del Juego
1.  **Selección de Categoría:** El niño elige entre tres grandes botones: **Números (1 al 20)**, **Letras Mayúsculas (A-Z)** o **Letras Minúsculas (a-z)**.
2.  **Catálogo:** Se despliega una cuadrícula visual para elegir la letra o número específico a practicar (ej. tocar la letra **B**).
3.  **Pantalla de Trazado:** El elemento seleccionado aparece gigante en el centro, compuesto por puntos grises claros con flechas guía que indican la dirección del trazo.

### 🎮 Mecánicas de Juego y UX Infantil
* **Modo Imán (Tolerancia):** El trazo del niño se magnetiza automáticamente al punto correcto si pasa cerca, evitando la frustración si no tiene un pulso perfecto.
* **Punto Activo:** El siguiente punto en el orden lógico parpadeará de un color llamativo (Amarillo/Rojo) para guiar al niño.
* **Refuerzo Auditivo:** Cada trazo exitoso reproduce el sonido de la letra o número en voz alta (*"¡P de Perro!"* o *"¡Cinco!"*), reforzando la enseñanza con audio y estímulo sonoro.
* **Retroalimentación al Completar:** Al terminar la secuencia de puntos al 100%, la app muestra una animación de confeti y queda preparada la zona para colorear. El contorno definitivo no se revela hasta que el niño presiona el botón **Dibujar**.
* **Botón Dibujar:** En lugar de habilitar inmediatamente el modo de colorear, se muestra un botón gigante **Dibujar**. Solo al presionarlo el niño puede comenzar a pintar el interior y el contorno negro del número o letra aparece para colorear.
* **Diseño del panel de coloreo:** El área de trazo principal ocupa todo el ancho disponible; la paleta de colores y herramientas se agrupan **debajo del lienzo**, en una fila horizontal desplazable (`colorToolsPanel`, ahora `orientation="horizontal"` dentro de un `HorizontalScrollView`).
* **Botones de color:** Cada botón usa el color que representa (Rojo, Azul, Amarillo, Verde, Morado, Café, Rosa y Negro) para mejorar la asociación visual. Tienen forma de **rectángulo redondeado** (`pencil_button_bg`, 52×84dp) en vez de círculo, para dar la impresión de ser lápices de colores.
* **Botón de color negro:** Se añade un botón adicional para pintar con color negro y trazar detalles más definidos.
* **Botón de color rosa:** Se añade un botón adicional (`btnColorPink`, `@color/pink`) junto al resto de la paleta.
* **Botón de pintar en blanco:** El penúltimo botón (`btnColorWhite`) ya no es un borrador con icono de escoba; ahora es un **círculo blanco con borde negro** (`white_circle_border_bg`) que simplemente pinta el dibujo de blanco con el mismo mecanismo que el resto de los colores. Se eliminó el modo borrador (`setEraserMode`/`eraserEnabled`/`eraserPaint`) de `TraceView`, ya que quedó redundante con este botón.
* **Botón Undo:** El último botón (`btnRevertColoring`) usa el mismo círculo blanco con borde negro que el botón anterior, con el icono de **cesto de basura** en negro (antes blanco sobre fondo negro), para la acción de volver a colorear.
* **Transición limpia:** Al activar el modo de colorear se quita el confeti y se oculta el rastro de puntos trazados, dejando visible solo el área del número o letra para pintar.
* **Nota de diseño:** El número se muestra ahora en el lado izquierdo con la paleta de colores a la derecha; esto facilita el seguimiento visual y deja más espacio para un trazo grande y claro.
* **Coloreado del Centro:** El niño puede elegir entre múltiples colores para pintar el interior del número o letra.
* **Navegación dentro del módulo:** Se mantiene una fila separada con los botones **Volver a selección** e **Inicio**, separados claramente del panel de colores.
* En la pantalla de selección de "Une los Puntos", el botón **Ir al inicio** debe aparecer al final, debajo de la cuadrícula de números o letras, y debe estar centrado horizontalmente.
* Se desactiva el estilo de texto en mayúsculas forzado en los botones del módulo para que las etiquetas respeten su formato natural y aparezcan en minúsculas/capitalización normal.
* Se eliminó el icono extra de navegación; ahora sólo existe el botón **Ir al inicio** para regresar al menú principal.

### 📷 Función: Subir Imágenes con Asociación Visual
Para integrar la subida de imágenes en este módulo pedagógico sin usar algoritmos de detección complejos, se utilizará el método de **Asociación de Objetos**:
* **Para el Abecedario:** Si se sube la foto de un *Perro*, la app la asociará a la letra **P**. Mostrará la foto del perro en tamaño pequeño en una esquina superior como referencia, y en el centro aparecerá la **P** gigante en puntos para trazar.
* **Para los Números:** Si se sube una foto con *5 manzanas*, la app mostrará la imagen como referencia y centrará el número **5** gigante en puntos para que el niño trace mientras cuenta los objetos de la foto.

---

## ✏️🌈 3. Módulo de Creatividad: Pizarra Libre

Este espacio actúa como un pizarrón en blanco diseñado para que el niño dibuje desde cero utilizando herramientas digitales básicas pero intuitivas.

### 🔁 Experiencia de Usuario Simplificada
* Al seleccionar **Pizarra Libre** desde el menú principal, se despliega **directamente** una pizarra completamente en blanco.
* **Sin pasos intermedios:** No hay que seleccionar números o letras; el lienzo está listo para dibujar de inmediato.
* El niño tiene acceso inmediato a la paleta de colores ubicada a la derecha para comenzar a crear sin restricciones.
* **Flujo lineal:** Menú Principal → **Pizarra Libre** → Lienzo en blanco con colores disponibles.
* **Dibujo activo desde el primer toque:** El modo de coloreo se habilita automáticamente en cuanto el lienzo aparece, sin necesidad de presionar **Dibujar** (a diferencia del módulo *Une los Puntos*). El niño selecciona un color y pinta deslizando el dedo sobre la pizarra; un toque simple también deja marca.

### 📐 Diseño de la Interfaz
* **Tablero Blanco:** Un lienzo completamente limpio que ocupa todo el ancho de la pantalla central.
* **Paleta Horizontal (Debajo del Lienzo):** La barra de colores gigantes se ubica **debajo del rectángulo de dibujo**, en una fila horizontal desplazable (antes estaba a la derecha, en columna vertical). Colores disponibles: Rojo, Azul, Amarillo, Verde, Morado, Café, Rosa y Negro, con forma de **rectángulo redondeado** para dar la impresión de lápices de colores (en vez de círculos).
* **Selector de Grosor (Inferior):** 3 botones simples con círculos de diferentes tamaños (Pequeño, Mediano, Grande) para que puedan cambiar el ancho de la línea de su trazo de forma visual. *(Pendiente de implementar en la UI, ver sección de estado.)*

### 🛠️ Herramientas Creativas Básicas
* **Modo Lápiz / Pincel:** Activado por defecto para trazar libremente con el dedo. La activación se realiza dentro de `onSizeChanged`/`setFreeMode` del `TraceView`, una vez que el lienzo ya tiene dimensiones reales y el bitmap de relleno está listo; así el trazo responde desde el primer toque aunque el modo de coloreo se solicite antes de medir la vista.
* **Botón de pintar en blanco:** El penúltimo botón de la paleta es un **círculo blanco con borde negro** que pinta el dibujo de blanco, usando el mismo mecanismo que el resto de los colores (reemplaza al antiguo borrador con icono de escoba, cuya lógica de modo borrador se eliminó del código por quedar redundante).
* **Botón Deshacer:** Último botón de la paleta; círculo blanco con borde negro e icono de **cesto de basura en negro**, para volver a colorear y eliminar el contenido anterior.
* **Botón Ir al Inicio:** Ubicado al final para que el niño regrese al menú principal cuando termine de dibujar.
* **Guardar en la Nevera:** Los dibujos creados desde cero se guardan en el directorio seleccionado asignándoles automáticamente un fondo blanco.


---

## 💾 4. Gestión de Almacenamiento y Tarjeta SD (Menú de Padres)

Para proteger la memoria interna principal del dispositivo y garantizar la seguridad en la navegación de archivos:

### ⚙️ Panel de Configuración Técnico
* Acceso protegido mediante un bloqueo parental (ej. resolver una suma simple) destinado únicamente al adulto.
* **Selector de Ubicación de Recursos:** Permite al padre alternar el almacenamiento de la app entre:
    * `Memoria Interna del Dispositivo`
    * `Tarjeta MicroSD Externa` (Recomendado para no consumir almacenamiento principal).

### 📁 El Baúl de Dibujos e Importación
* La aplicación apuntará a una carpeta específica llamada `/DibujosParaColorear/` ubicada dinámicamente en la raíz de la opción seleccionada (Interna o SD).
* El padre es el encargado de descargar y colocar ahí las plantillas en blanco y negro para que los niños las vean organizadas dentro de la app.
* **Optimización de Espacio:** Los dibujos guardados por los niños se exportarán en formato JPEG comprimido (calidad 80%) para minimizar el impacto en los bytes de almacenamiento de la tarjeta o memoria.

---

## 🛠️ Roadmap Técnico de Implementación (Sugerido para Android Nativo)

* **Fase 1: Configuración de Rutas y Almacenamiento.** Programar la detección de la tarjeta MicroSD (`Context.getExternalFilesDirs()`) y crear el menú de configuración parental para alternar la ruta de guardado.
* **Fase 2: Motor del Lienzo (Canvas Base y Pizarra).** Configurar la vista táctil que permita pintar con el dedo en base a coordenadas sobre un fondo blanco y cambiar el color/grosor del pincel.
* **Fase 3: Interfaz Infantil y Control de Herramientas.** Diseñar los botones gigantes, barra lateral y programar la lógica del botón **Reset**, **Borrador** y el **Candado de Seguridad**.
* **Fase 4: Módulo de Trazado (Letras/Números).** Implementar las fuentes de puntos guía en formato gigante, la lógica de orden secuencial (1-20, A-Z) y los efectos de audio de asociación.