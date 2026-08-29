# Guía: Agregar Nuevas Plantillas al Módulo de Colorear

## Descripción General
Este documento explica cómo agregar nuevas plantillas (imágenes) al módulo de colorear de la app Kids Games. Usaremos como ejemplo la plantilla **Bumpy**.

## Requisito Crítico: Fondo Transparente

⚠️ **IMPORTANTE:** Cualquier imagen que se agregue DEBE tener un fondo transparente, NO un fondo blanco sólido.

### ¿Por qué?
El sistema de coloreo de `ColoringView` dibuja en este orden:
1. **fillBitmap** (lo que el niño dibuja)
2. **templateDrawable** (la plantilla encima)

Si la plantilla tiene un fondo blanco opaco, cubrirá todos los trazos del niño haciendo que el color aparezca **debajo** de la imagen en lugar de **sobre** ella.

## Pasos para Agregar una Nueva Plantilla

### Paso 1: Obtener la Imagen
Coloca el archivo PNG de la plantilla en una ubicación accesible. En este ejemplo:
```
app/build/bumpy.png
```

### Paso 2: Copiar a Recursos
Copia la imagen al directorio de recursos drawable-nodpi:
```bash
cp app/build/bumpy.png app/src/main/res/drawable-nodpi/template_bumpy.png
```

**Nota:** Usa la carpeta `drawable-nodpi` para imágenes de alta resolución que no necesitan escalado automático.

### Paso 3: Convertir Fondo Blanco a Transparente

Este es el paso CRÍTICO. Usa Python con PIL para procesar la imagen:

```python
from PIL import Image

img_path = "app/src/main/res/drawable-nodpi/template_bumpy.png"
img = Image.open(img_path)

# Convertir a RGBA (con canal alpha/transparencia)
if img.mode != 'RGBA':
    img = img.convert('RGBA')

# Procesar cada píxel
data = img.getdata()
new_data = []

for pixel in data:
    # Si el píxel es blanco o casi blanco (RGB > 240)
    if pixel[0] > 240 and pixel[1] > 240 and pixel[2] > 240:
        # Hacerlo transparente: (R, G, B, Alpha=0)
        new_data.append((255, 255, 255, 0))
    else:
        # Mantener otros píxeles
        new_data.append(pixel)

img.putdata(new_data)
img.save(img_path, 'PNG')
```

**Resultado:**
- ✅ Formato: RGBA
- ✅ Fondo blanco: Convertido a transparente
- ✅ Detalles: Preservados

### Paso 4: Agregar a la Lista de Plantillas

Edita `app/src/main/java/com/example/kidsgames/ColoringActivity.kt`:

```kotlin
private val templates = listOf(
    "Bumpy" to R.drawable.template_bumpy,        // ← Nueva plantilla
    "Esqueleto" to R.drawable.template_esqueleto,
    "Bob" to R.drawable.template_bob,
    "Pocoyo" to R.drawable.template_pocoyo,
    // ... resto de plantillas
)
```

**Ubicación:** Línea 45-55 en `ColoringActivity.kt`

### Paso 5: Compilar e Instalar

```bash
./gradlew clean assembleDebug
adb install -r app/build/outputs/apk/debug/kids-games.apk
```

### Paso 6: Probar

1. Abre la app
2. Selecciona "Colorear"
3. El botón "Bumpy" debe aparecer como **primera opción** (antes de Esqueleto)
4. Selecciona "Bumpy"
5. Verifica que puedas pintar **sobre la imagen** correctamente

## Checklist de Implementación

- [ ] Imagen PNG obtenida
- [ ] Copiada a `app/src/main/res/drawable-nodpi/template_[nombre].png`
- [ ] Fondo blanco convertido a transparente (¡CRÍTICO!)
- [ ] Verificado formato RGBA
- [ ] Entrada agregada a lista `templates` en ColoringActivity.kt
- [ ] Compilación exitosa sin errores
- [ ] Probado en emulador o dispositivo
- [ ] Confirmado que se colorea **sobre** la imagen

## Sintaxis de Nombres

Para mantener consistencia:
- **Archivos:** `template_[nombre].png` (minúsculas, guiones bajos)
- **Recursos R:** `R.drawable.template_[nombre]`
- **Lista:** `"[Nombre]" to R.drawable.template_[nombre]` (nombre capitalizado)

## Ejemplos de Plantillas Actuales

| Nombre | Archivo | Estado |
|--------|---------|--------|
| Bumpy | `template_bumpy.png` | ✅ Con fondo transparente |
| Esqueleto | `template_esqueleto.png` | ✅ Con fondo transparente |
| Bob | `template_bob.png` | ✅ Con fondo transparente |
| Pocoyo | `template_pocoyo.png` | ✅ Con fondo transparente |
| Flor | `template_flower.xml` | ✅ Vector XML |
| Corazón | `template_heart.xml` | ✅ Vector XML |
| Casa | `template_house.xml` | ✅ Vector XML |
| Estrella | `template_star.xml` | ✅ Vector XML |

## Solución de Problemas

### Problema: El color se aplica debajo de la imagen, no sobre
**Causa:** El fondo blanco NO fue convertido a transparente
**Solución:** Ejecuta el script Python del Paso 3 nuevamente

### Problema: La imagen se ve pixelada
**Causa:** La imagen está en baja resolución
**Solución:** Usa imágenes de alta resolución (mínimo 1000x1000 píxeles)

### Problema: Compilation error: R.drawable.template_[nombre] no existe
**Causa:** 
  1. El archivo PNG no está en la carpeta correcta
  2. El nombre del archivo no coincide con la referencia en el código
**Solución:** Verifica que el archivo esté exactamente en `app/src/main/res/drawable-nodpi/` y que el nombre sea idéntico

## Notas Técnicas

### ¿Por qué drawable-nodpi?
- `drawable/` - Usado para imágenes que deben escalarse según densidad
- `drawable-nodpi/` - Usado para imágenes que NO deben escalarse automáticamente
- Para plantillas de colorear usamos `drawable-nodpi` porque queremos mantener la resolución exacta

### Formato RGBA
- **RGB:** Red, Green, Blue (3 canales de color)
- **RGBA:** RGB + Alpha (4 canales: color + transparencia)
- Alpha = 0 → Completamente transparente
- Alpha = 255 → Completamente opaco

### ColoringView Rendering
```
Canvas
├── fillBitmap (lienzo del niño)     ← Aquí van los trazos
└── templateDrawable (plantilla)     ← Encima para "capa inteligente"
```

## Commits Relacionados

- `feat: Agregar plantilla Esqueleto` - Primera plantilla con fondo transparente
- `feat: Agregar plantilla Bumpy` - Segunda plantilla con fondo transparente

---

**Última actualización:** 28 de Agosto de 2026
**Versión:** 2.0
**Estado:** ✅ Documentado y Testeado
