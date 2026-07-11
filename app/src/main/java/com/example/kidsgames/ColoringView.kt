package com.example.kidsgames

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Lienzo del **Módulo 1: ¡A Colorear!**
 *
 * Muestra una plantilla de contorno (dibujo lineal negro) y permite pintar
 * sobre ella implementando la **Capa de Trazo Inteligente** descrita en la
 * especificación: los trazos del niño se acumulan en [fillBitmap] y se dibujan
 * SIEMPRE *por debajo* del contorno negro de la plantilla, de modo que el
 * resultado respeta las líneas del dibujo original y siempre se ve limpio.
 *
 * Orden de renderizado en [onDraw] (de abajo hacia arriba):
 *  1. Fondo blanco.
 *  2. [fillBitmap] con el coloreado del niño.
 *  3. La plantilla de contorno negro ([templateDrawable]) encima.
 *
 * Soporta zoom ([zoomIn]/[zoomOut]) aplicado como escala uniforme alrededor
 * del centro, botones direccionales para mover el lienzo ampliado
 * ([panLeft]/[panRight]/[panUp]/[panDown], recortados para que nunca se separe
 * del marco, ver [clampTranslation]), reinicio ([reset]) y bloqueo
 * anti-errores ([locked]) que ignora los toques mientras está activo.
 */
class ColoringView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var templateDrawable: Drawable? = null

    private var fillBitmap: Bitmap? = null
    private var fillCanvas: Canvas? = null

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 16f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, android.R.color.holo_red_dark)
    }
    private var locked = false

    private var scaleFactor = 1f
    private val minScale = 1f
    private val maxScale = 3f

    private var lastX = -1f
    private var lastY = -1f
    private var isDrawing = false

    /** Se invoca cuando cambia el zoom o el desplazamiento, para refrescar las flechas. */
    var onViewportChanged: (() -> Unit)? = null

    /** Carga la plantilla de contorno a colorear y reinicia el lienzo. */
    fun setTemplate(resId: Int) {
        templateDrawable = ContextCompat.getDrawable(context, resId)
        if (width > 0 && height > 0) {
            prepareFillBitmap(width, height)
        }
        invalidate()
    }

    fun setBrushColor(color: Int) {
        linePaint.color = color
    }

    fun setLocked(value: Boolean) {
        locked = value
    }

    fun isLocked(): Boolean = locked

    fun zoomIn() {
        if (locked) return
        scaleFactor = (scaleFactor * 1.25f).coerceAtMost(maxScale)
        applyScale()
    }

    fun zoomOut() {
        if (locked) return
        scaleFactor = (scaleFactor / 1.25f).coerceAtLeast(minScale)
        applyScale()
    }

    private fun applyScale() {
        scaleX = scaleFactor
        scaleY = scaleFactor
        clampTranslation()
        onViewportChanged?.invoke()
    }

    /** Distancia fija (dp) que avanza cada toque de una flecha direccional. */
    private val panStep: Float
        get() = 48f * resources.displayMetrics.density

    // Nota: mover hacia una dirección debe REVELAR ese lado del dibujo (p. ej.
    // presionar "izquierda" muestra la parte izquierda de la imagen), por lo
    // que el contenido se traslada en el sentido contrario a la flecha.
    fun panLeft() {
        if (locked) return
        translationX += panStep
        clampTranslation()
        invalidate()
        onViewportChanged?.invoke()
    }

    fun panRight() {
        if (locked) return
        translationX -= panStep
        clampTranslation()
        invalidate()
        onViewportChanged?.invoke()
    }

    fun panUp() {
        if (locked) return
        translationY += panStep
        clampTranslation()
        invalidate()
        onViewportChanged?.invoke()
    }

    fun panDown() {
        if (locked) return
        translationY -= panStep
        clampTranslation()
        invalidate()
        onViewportChanged?.invoke()
    }

    private fun maxPanX() = (width * (scaleFactor - 1f) / 2f).coerceAtLeast(0f)
    private fun maxPanY() = (height * (scaleFactor - 1f) / 2f).coerceAtLeast(0f)

    /** true si todavía queda margen para mover el lienzo ampliado en esa dirección. */
    fun canPanLeft(): Boolean = translationX < maxPanX() - 0.5f
    fun canPanRight(): Boolean = translationX > -maxPanX() + 0.5f
    fun canPanUp(): Boolean = translationY < maxPanY() - 0.5f
    fun canPanDown(): Boolean = translationY > -maxPanY() + 0.5f

    /**
     * Limita [translationX]/[translationY] para que el lienzo ampliado nunca se
     * despegue del marco naranja: como máximo se puede desplazar la mitad del
     * sobrante que generó el zoom en cada eje. Sin zoom (escala 1×) el máximo es
     * cero, así que no hay movimiento posible.
     */
    private fun clampTranslation() {
        val maxPanX = maxPanX()
        val maxPanY = maxPanY()
        translationX = translationX.coerceIn(-maxPanX, maxPanX)
        translationY = translationY.coerceIn(-maxPanY, maxPanY)
    }

    /** Borra todo el coloreado dejando solo la plantilla. */
    fun reset() {
        if (locked) return
        fillCanvas?.drawColor(Color.WHITE, PorterDuff.Mode.SRC)
        scaleFactor = 1f
        translationX = 0f
        translationY = 0f
        applyScale()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        prepareFillBitmap(w, h)
        onViewportChanged?.invoke()
    }

    private fun prepareFillBitmap(w: Int, h: Int) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE, PorterDuff.Mode.SRC)
        fillBitmap = bmp
        fillCanvas = c
    }

    private fun templateBounds(): android.graphics.Rect {
        // Ajusta el contorno dentro de un cuadro centrado (92% del lado menor)
        // respetando su relación de aspecto original, para no deformar plantillas
        // que no sean cuadradas (p. ej. imágenes importadas como Bob o Pocoyo).
        val maxSize = minOf(width, height) * 0.92f
        val drawable = templateDrawable
        val intrinsicW = drawable?.intrinsicWidth?.takeIf { it > 0 } ?: 1
        val intrinsicH = drawable?.intrinsicHeight?.takeIf { it > 0 } ?: 1
        val scale = maxSize / maxOf(intrinsicW, intrinsicH)
        val w = (intrinsicW * scale).toInt()
        val h = (intrinsicH * scale).toInt()
        val left = (width - w) / 2
        val top = (height - h) / 2
        return android.graphics.Rect(left, top, left + w, top + h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. Coloreado del niño (incluye su propio fondo blanco).
        fillBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        // 2. Plantilla de contorno negro SIEMPRE encima (capa de trazo inteligente).
        templateDrawable?.let {
            it.bounds = templateBounds()
            it.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (locked) return true
        val fc = fillCanvas ?: return true
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = x
                lastY = y
                isDrawing = true
                fc.drawCircle(x, y, linePaint.strokeWidth / 2f, linePaint)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrawing) {
                    if (lastX >= 0 && lastY >= 0) {
                        fc.drawLine(lastX, lastY, x, y, linePaint)
                    } else {
                        fc.drawCircle(x, y, linePaint.strokeWidth / 2f, linePaint)
                    }
                    lastX = x
                    lastY = y
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDrawing = false
                lastX = -1f
                lastY = -1f
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Devuelve un bitmap con el resultado final (coloreado + contorno) para
     * guardarlo en la "Nevera Virtual". Se renderiza a tamaño real de la vista.
     */
    fun exportBitmap(): Bitmap {
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.WHITE)
        fillBitmap?.let { c.drawBitmap(it, 0f, 0f, null) }
        templateDrawable?.let {
            it.bounds = templateBounds()
            it.draw(c)
        }
        return out
    }
}
