package com.example.kidsgames

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.speech.tts.TextToSpeech
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.hypot

/**
 * Vista personalizada que sirve para dos modos:
 *
 * 1. **Trazado (Une los Puntos):** muestra una letra/número gigante como puntos
 *    guía. El niño los recorre en orden; al completarlos se habilita el coloreo
 *    tras presionar **Dibujar** ([enableColoringMode]).
 * 2. **Pizarra libre:** activada con [setFreeMode]. Todo el lienzo es dibujable y
 *    el coloreo se habilita automáticamente, sin pasos previos.
 *
 * **El dibujo se acumula sobre [fillBitmap]** (respaldado por [fillCanvas]), un
 * mapa de bits del tamaño de la vista que se pinta en cada [onDraw]. Mientras el
 * dedo se desliza, [onTouchEvent] traza segmentos de línea de [lastX]/[lastY] al
 * punto actual con [linePaint], por
 * lo que el trazo persiste aunque la vista se redibuje.
 *
 * **Sincronización con el tamaño de la vista:** el bitmap solo puede crearse
 * cuando la vista ya tiene dimensiones. Por eso el coloreo se habilita en
 * [onSizeChanged]/[setFreeMode] (no antes), garantizando que el primer toque ya
 * pinte aunque la Activity solicite el modo de coloreo durante `onCreate`.
 */
class TraceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetChar: Char = 'A'
    private val points = mutableListOf<PointF>()
    private var visited = mutableListOf<Boolean>()
    // Índices de los puntos que INICIAN un nuevo trazo (contorno) del glifo. En letras
    // con varios trazos sueltos (p. ej. la A: dos diagonales + el travesaño) el dedo
    // debe "levantar la pluma" entre uno y otro. Sin esto, [tracePath] uniría con una
    // línea recta el final de un trazo con el comienzo del siguiente, dibujando trazos
    // azules que no pertenecen a la letra y confunden al niño.
    private val contourStarts = mutableSetOf<Int>()
    private var activeIndex = 0
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.trace_guide)
    }
    private val pointRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = ContextCompat.getColor(context, R.color.trace_guide_ring)
    }
    private val visitedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.trace_visited)
    }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.trace_active)
    }
    private val activeRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = ContextCompat.getColor(context, R.color.trace_active)
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.trace_arrow)
    }
    private val traceGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 34f
        color = ContextCompat.getColor(context, R.color.trace_line_glow)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val tracePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        color = ContextCompat.getColor(context, R.color.trace_line)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    // Al completar el trazado, el número queda en un único trazo verde y limpio
    // (sin los puntos guía encima), como recompensa visual antes de "Dibujar".
    private val traceDoneGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 44f
        color = ContextCompat.getColor(context, R.color.trace_visited)
        alpha = 70
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val traceDonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 30f
        color = ContextCompat.getColor(context, R.color.trace_visited)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        color = Color.BLACK
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
        alpha = 120
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 22f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, android.R.color.holo_blue_light)
        alpha = 180
    }
    private val fillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    private val confettiPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var blinkAlpha = 255
    // Radio de tolerancia del imán: el dedo debe pasar a esta distancia (px) del
    // SIGUIENTE punto pendiente para activarlo. Antes era 80f; se reduce para que
    // el niño tenga que recorrer la línea de verdad y no se "complete sola".
    private var magnetRadius = 55f
    // Última posición del dedo en modo trazado, para medir el tramo recorrido entre
    // dos eventos de movimiento (no solo el punto final). Vale -1 si no hay tramo.
    private var traceLastX = -1f
    private var traceLastY = -1f
    private var path = Path()
    private var tracePath = Path()
    private val confettiParticles = mutableListOf<ConfettiParticle>()
    private var confettiAnimator: ValueAnimator? = null
    private var completed = false
    private var coloringEnabled = false
    private var freeMode = false
    private var fillBitmap: Bitmap? = null
    private var fillCanvas: Canvas? = null
    private var pathRegion: Region = Region()
    private var lastX = -1f
    private var lastY = -1f
    private var isDrawing = false

    private var tts: TextToSpeech? = null
    private var onComplete: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // App infantil en español: forzar voz en español, con respaldos.
                val spanishLocales = listOf(
                    Locale("es", "ES"),
                    Locale("es", "MX"),
                    Locale("es")
                )
                val chosen = spanishLocales.firstOrNull {
                    val r = tts?.isLanguageAvailable(it)
                    r == TextToSpeech.LANG_AVAILABLE ||
                        r == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                        r == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
                }
                if (chosen != null) {
                    tts?.language = chosen
                } else {
                    // No hay voz en español instalada: usar la del sistema y avisar en Logcat.
                    tts?.language = Locale.getDefault()
                    Log.w(
                        "TraceView",
                        "Voz en español NO disponible en el TTS. " +
                            "Instala 'Español' en Configuración > Sistema > Idiomas > " +
                            "Texto a voz (Google) para escuchar el refuerzo auditivo."
                    )
                }
            } else {
                Log.w("TraceView", "Inicialización de TextToSpeech falló (status=$status).")
            }
        }
    }

    fun setOnCompleteListener(cb: () -> Unit) {
        onComplete = cb
    }

    fun setTargetChar(c: Char) {
        targetChar = c
        rebuild()
    }

    private fun rebuild() {
        points.clear()
        visited.clear()
        contourStarts.clear()
        activeIndex = 0
        traceLastX = -1f
        traceLastY = -1f
        path.reset()
        tracePath.reset()
        completed = false
        coloringEnabled = false
        confettiAnimator?.cancel()
        confettiParticles.clear()
        fillBitmap = null
        fillCanvas = null
        pathRegion.setEmpty()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == 0 || h == 0) return
        if (freeMode) {
            // Pizarra libre: la región de coloreado ocupa todo el lienzo
            prepareFillBitmap(w, h)
            completed = true
            pathRegion.set(0, 0, w, h)
            // El lienzo ya tiene tamaño: habilitar el dibujo aunque
            // enableColoringMode() se hubiera llamado antes de medir la vista.
            coloringEnabled = true
            invalidate()
            return
        }

        buildPointsForChar()
        prepareFillBitmap(w, h)
        startBlink()
    }

    /** Activa o desactiva el modo pizarra libre. */
    fun setFreeMode(enabled: Boolean) {
        freeMode = enabled
        if (enabled && width > 0 && height > 0) {
            prepareFillBitmap(width, height)
            completed = true
            pathRegion.set(0, 0, width, height)
            coloringEnabled = true
        }
        invalidate()
    }

    private fun buildPointsForChar() {
        // El contorno relleno del glifo (path) se usa SOLO para el dibujo negro
        // final y el coloreado interior. Los puntos a trazar se muestrean sobre
        // el ESQUELETO central del trazo, que es por donde el niño realmente desliza.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        paint.textSize = height * 0.82f
        val text = targetChar.toString()
        val bounds = Rect()
        paint.getTextBounds(text, 0, 1, bounds)
        val glyphX = width / 2f - paint.measureText(text) / 2f
        val glyphY = height / 2f + bounds.height() / 2f
        paint.getTextPath(text, 0, 1, glyphX, glyphY, path)
        path.fillType = Path.FillType.EVEN_ODD

        val boundsF = RectF()
        path.computeBounds(boundsF, true)
        pathRegion.setPath(
            path,
            Region(
                boundsF.left.toInt().coerceAtMost(0),
                boundsF.top.toInt().coerceAtMost(0),
                boundsF.right.toInt().coerceAtLeast(width),
                boundsF.bottom.toInt().coerceAtLeast(height)
            )
        )

        // Camino a trazar: esqueleto central si existe; si no, respaldo al contorno.
        val skeleton = buildSkeletonPath(boundsF)
        val tracePathSource = skeleton ?: path

        val pm = PathMeasure(tracePathSource, false)
        val numPoints = 28
        val pos = FloatArray(2)
        // PathMeasure solo mide un contorno por vez; recorrer todos para soportar
        // esqueletos con varios trazos (p. ej. el 4 o el 5), repartiendo los puntos
        // de forma proporcional a la longitud de cada contorno.
        val contours = mutableListOf<Float>()
        var total = 0f
        do {
            val l = pm.length
            contours.add(l)
            total += l
        } while (pm.nextContour())
        if (total <= 0f) {
            // Camino degenerado: colocar un único punto al centro para no fallar.
            points.add(PointF(width / 2f, height / 2f))
            visited.add(false)
            return
        }

        pm.setPath(tracePathSource, false)
        var contourIdx = 0
        var remaining = (numPoints * (contours[0] / total)).toInt().coerceAtLeast(1)
        var emittedInContour = 0
        for (i in 0 until numPoints) {
            // Avanzar de contorno cuando se agotan los puntos asignados al actual.
            while (emittedInContour >= remaining && contourIdx < contours.size - 1) {
                pm.nextContour()
                contourIdx++
                emittedInContour = 0
                remaining = if (contourIdx == contours.size - 1) {
                    numPoints - points.size
                } else {
                    (numPoints * (contours[contourIdx] / total)).toInt().coerceAtLeast(1)
                }
            }
            val segLen = contours[contourIdx]
            val denom = (remaining - 1).coerceAtLeast(1)
            val dist = if (remaining <= 1) 0f else segLen * emittedInContour / denom
            pm.getPosTan(dist, pos, null)
            // El primer punto emitido de cada contorno marca un trazo nuevo, para que
            // tracePath levante la pluma allí en vez de unir trazos con una línea recta.
            if (emittedInContour == 0) contourStarts.add(points.size)
            points.add(PointF(pos[0], pos[1]))
            visited.add(false)
            emittedInContour++
        }
    }

    /**
     * Construye el camino central (esqueleto) a partir de polilíneas normalizadas
     * en [0,1] x [0,1] definidas para el carácter, escaladas a los límites reales
     * del glifo [bounds]. Devuelve null si el carácter no tiene esqueleto definido,
     * para que el llamador use el contorno como respaldo.
     */
    private fun buildSkeletonPath(bounds: RectF): Path? {
        val strokes = skeletonFor(targetChar) ?: return null
        val p = Path()
        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            for ((i, pt) in stroke.withIndex()) {
                val sx = bounds.left + pt.x * bounds.width()
                val sy = bounds.top + pt.y * bounds.height()
                if (i == 0) p.moveTo(sx, sy) else p.lineTo(sx, sy)
            }
        }
        return p
    }

    /**
     * Esqueletos de trazo por carácter, como lista de trazos; cada trazo es una
     * polilínea de puntos normalizados (0..1) relativos al recuadro del glifo.
     * El orden de los puntos define la dirección correcta del trazado.
     * Se cubren los dígitos 0–9, las mayúsculas A–Z y las minúsculas a–z. Las
     * minúsculas se normalizan al recuadro REAL del glifo (ajustado por
     * [PathMeasure]/`computeBounds`), que ya incluye ascendentes y descendentes;
     * por eso, p. ej., el bucle de la `b` ocupa solo la mitad inferior y la cola de
     * la `g` la inferior. Cualquier otro carácter cae al respaldo del contorno.
     */
    private fun skeletonFor(c: Char): List<List<PointF>>? {
        fun pt(x: Float, y: Float) = PointF(x, y)
        return when (c) {
            '0' -> listOf(
                listOf(
                    pt(0.5f, 0.02f), pt(0.18f, 0.18f), pt(0.05f, 0.5f),
                    pt(0.18f, 0.82f), pt(0.5f, 0.98f), pt(0.82f, 0.82f),
                    pt(0.95f, 0.5f), pt(0.82f, 0.18f), pt(0.5f, 0.02f)
                )
            )
            '1' -> listOf(
                listOf(pt(0.25f, 0.2f), pt(0.55f, 0.02f), pt(0.55f, 0.98f)),
                listOf(pt(0.2f, 0.98f), pt(0.9f, 0.98f))
            )
            '2' -> listOf(
                listOf(
                    pt(0.12f, 0.22f), pt(0.5f, 0.02f), pt(0.85f, 0.22f),
                    pt(0.7f, 0.5f), pt(0.15f, 0.98f), pt(0.9f, 0.98f)
                )
            )
            '3' -> listOf(
                listOf(
                    pt(0.12f, 0.18f), pt(0.5f, 0.02f), pt(0.85f, 0.2f),
                    pt(0.5f, 0.5f), pt(0.88f, 0.75f), pt(0.5f, 0.98f), pt(0.12f, 0.82f)
                )
            )
            '4' -> listOf(
                listOf(pt(0.68f, 0.02f), pt(0.1f, 0.68f), pt(0.95f, 0.68f)),
                listOf(pt(0.68f, 0.25f), pt(0.68f, 0.98f))
            )
            '5' -> listOf(
                listOf(
                    pt(0.85f, 0.04f), pt(0.22f, 0.04f), pt(0.2f, 0.45f),
                    pt(0.55f, 0.4f), pt(0.85f, 0.62f), pt(0.6f, 0.96f),
                    pt(0.18f, 0.9f)
                )
            )
            '6' -> listOf(
                listOf(
                    pt(0.78f, 0.06f), pt(0.35f, 0.25f), pt(0.18f, 0.6f),
                    pt(0.25f, 0.88f), pt(0.55f, 0.98f), pt(0.82f, 0.82f),
                    pt(0.82f, 0.58f), pt(0.5f, 0.48f), pt(0.22f, 0.6f)
                )
            )
            '7' -> listOf(
                listOf(pt(0.12f, 0.05f), pt(0.9f, 0.05f), pt(0.45f, 0.98f))
            )
            '8' -> listOf(
                listOf(
                    pt(0.5f, 0.5f), pt(0.22f, 0.32f), pt(0.4f, 0.06f),
                    pt(0.6f, 0.06f), pt(0.78f, 0.32f), pt(0.5f, 0.5f),
                    pt(0.2f, 0.72f), pt(0.4f, 0.96f), pt(0.6f, 0.96f),
                    pt(0.8f, 0.72f), pt(0.5f, 0.5f)
                )
            )
            '9' -> listOf(
                listOf(
                    pt(0.78f, 0.42f), pt(0.5f, 0.52f), pt(0.2f, 0.4f),
                    pt(0.25f, 0.16f), pt(0.55f, 0.04f), pt(0.8f, 0.2f),
                    pt(0.78f, 0.55f), pt(0.6f, 0.9f), pt(0.25f, 0.98f)
                )
            )
            // Mayúsculas: línea central de cada trazo, en el orden de escritura
            // habitual (de arriba abajo / izquierda a derecha). Cada sublista es un
            // trazo independiente; el dedo los recorre en secuencia.
            'A' -> listOf(
                listOf(pt(0.5f, 0.02f), pt(0.12f, 0.98f)),
                listOf(pt(0.5f, 0.02f), pt(0.88f, 0.98f)),
                listOf(pt(0.28f, 0.6f), pt(0.72f, 0.6f))
            )
            'B' -> listOf(
                listOf(pt(0.15f, 0.98f), pt(0.15f, 0.02f)),
                listOf(
                    pt(0.15f, 0.02f), pt(0.6f, 0.06f), pt(0.82f, 0.27f),
                    pt(0.55f, 0.49f), pt(0.15f, 0.5f)
                ),
                listOf(
                    pt(0.15f, 0.5f), pt(0.62f, 0.53f), pt(0.86f, 0.74f),
                    pt(0.58f, 0.96f), pt(0.15f, 0.98f)
                )
            )
            'C' -> listOf(
                listOf(
                    pt(0.85f, 0.2f), pt(0.58f, 0.05f), pt(0.28f, 0.12f),
                    pt(0.12f, 0.4f), pt(0.12f, 0.6f), pt(0.28f, 0.88f),
                    pt(0.58f, 0.95f), pt(0.85f, 0.8f)
                )
            )
            'D' -> listOf(
                listOf(pt(0.15f, 0.98f), pt(0.15f, 0.02f)),
                listOf(
                    pt(0.15f, 0.02f), pt(0.55f, 0.05f), pt(0.85f, 0.35f),
                    pt(0.85f, 0.65f), pt(0.55f, 0.95f), pt(0.15f, 0.98f)
                )
            )
            'E' -> listOf(
                listOf(
                    pt(0.85f, 0.05f), pt(0.15f, 0.05f),
                    pt(0.15f, 0.98f), pt(0.85f, 0.98f)
                ),
                listOf(pt(0.15f, 0.5f), pt(0.7f, 0.5f))
            )
            'F' -> listOf(
                listOf(pt(0.85f, 0.05f), pt(0.15f, 0.05f), pt(0.15f, 0.98f)),
                listOf(pt(0.15f, 0.5f), pt(0.7f, 0.5f))
            )
            'G' -> listOf(
                listOf(
                    pt(0.85f, 0.2f), pt(0.58f, 0.05f), pt(0.28f, 0.12f),
                    pt(0.12f, 0.4f), pt(0.12f, 0.6f), pt(0.28f, 0.88f),
                    pt(0.58f, 0.95f), pt(0.85f, 0.78f), pt(0.85f, 0.55f),
                    pt(0.6f, 0.55f)
                )
            )
            'H' -> listOf(
                listOf(pt(0.15f, 0.02f), pt(0.15f, 0.98f)),
                listOf(pt(0.85f, 0.02f), pt(0.85f, 0.98f)),
                listOf(pt(0.15f, 0.5f), pt(0.85f, 0.5f))
            )
            'I' -> listOf(
                listOf(pt(0.25f, 0.05f), pt(0.75f, 0.05f)),
                listOf(pt(0.5f, 0.05f), pt(0.5f, 0.98f)),
                listOf(pt(0.25f, 0.98f), pt(0.75f, 0.98f))
            )
            'J' -> listOf(
                listOf(
                    pt(0.7f, 0.05f), pt(0.7f, 0.72f), pt(0.52f, 0.94f),
                    pt(0.27f, 0.86f), pt(0.18f, 0.65f)
                )
            )
            'K' -> listOf(
                listOf(pt(0.15f, 0.02f), pt(0.15f, 0.98f)),
                listOf(pt(0.85f, 0.05f), pt(0.15f, 0.52f)),
                listOf(pt(0.32f, 0.42f), pt(0.85f, 0.98f))
            )
            'L' -> listOf(
                listOf(pt(0.2f, 0.05f), pt(0.2f, 0.98f), pt(0.85f, 0.98f))
            )
            'M' -> listOf(
                listOf(
                    pt(0.1f, 0.98f), pt(0.1f, 0.05f), pt(0.5f, 0.62f),
                    pt(0.9f, 0.05f), pt(0.9f, 0.98f)
                )
            )
            'N' -> listOf(
                listOf(
                    pt(0.15f, 0.98f), pt(0.15f, 0.05f),
                    pt(0.85f, 0.98f), pt(0.85f, 0.05f)
                )
            )
            'O' -> listOf(
                listOf(
                    pt(0.5f, 0.04f), pt(0.2f, 0.2f), pt(0.1f, 0.5f),
                    pt(0.2f, 0.8f), pt(0.5f, 0.96f), pt(0.8f, 0.8f),
                    pt(0.9f, 0.5f), pt(0.8f, 0.2f), pt(0.5f, 0.04f)
                )
            )
            'P' -> listOf(
                listOf(pt(0.15f, 0.98f), pt(0.15f, 0.02f)),
                listOf(
                    pt(0.15f, 0.02f), pt(0.6f, 0.06f), pt(0.84f, 0.28f),
                    pt(0.6f, 0.5f), pt(0.15f, 0.52f)
                )
            )
            'Q' -> listOf(
                listOf(
                    pt(0.5f, 0.04f), pt(0.2f, 0.2f), pt(0.1f, 0.5f),
                    pt(0.2f, 0.8f), pt(0.5f, 0.96f), pt(0.8f, 0.8f),
                    pt(0.9f, 0.5f), pt(0.8f, 0.2f), pt(0.5f, 0.04f)
                ),
                listOf(pt(0.6f, 0.68f), pt(0.95f, 0.98f))
            )
            'R' -> listOf(
                listOf(pt(0.15f, 0.98f), pt(0.15f, 0.02f)),
                listOf(
                    pt(0.15f, 0.02f), pt(0.6f, 0.06f), pt(0.84f, 0.28f),
                    pt(0.6f, 0.5f), pt(0.15f, 0.52f)
                ),
                listOf(pt(0.4f, 0.52f), pt(0.85f, 0.98f))
            )
            'S' -> listOf(
                listOf(
                    pt(0.82f, 0.2f), pt(0.55f, 0.06f), pt(0.25f, 0.12f),
                    pt(0.2f, 0.32f), pt(0.45f, 0.46f), pt(0.7f, 0.58f),
                    pt(0.78f, 0.76f), pt(0.5f, 0.94f), pt(0.18f, 0.82f)
                )
            )
            'T' -> listOf(
                listOf(pt(0.1f, 0.05f), pt(0.9f, 0.05f)),
                listOf(pt(0.5f, 0.05f), pt(0.5f, 0.98f))
            )
            'U' -> listOf(
                listOf(
                    pt(0.15f, 0.05f), pt(0.17f, 0.68f), pt(0.35f, 0.92f),
                    pt(0.5f, 0.96f), pt(0.65f, 0.92f), pt(0.83f, 0.68f),
                    pt(0.85f, 0.05f)
                )
            )
            'V' -> listOf(
                listOf(pt(0.1f, 0.05f), pt(0.5f, 0.98f), pt(0.9f, 0.05f))
            )
            'W' -> listOf(
                listOf(
                    pt(0.08f, 0.05f), pt(0.28f, 0.98f), pt(0.5f, 0.4f),
                    pt(0.72f, 0.98f), pt(0.92f, 0.05f)
                )
            )
            'X' -> listOf(
                listOf(pt(0.12f, 0.05f), pt(0.88f, 0.98f)),
                listOf(pt(0.88f, 0.05f), pt(0.12f, 0.98f))
            )
            'Y' -> listOf(
                listOf(pt(0.12f, 0.05f), pt(0.5f, 0.5f)),
                listOf(pt(0.88f, 0.05f), pt(0.5f, 0.5f)),
                listOf(pt(0.5f, 0.5f), pt(0.5f, 0.98f))
            )
            'Z' -> listOf(
                listOf(
                    pt(0.12f, 0.05f), pt(0.88f, 0.05f),
                    pt(0.12f, 0.98f), pt(0.88f, 0.98f)
                )
            )
            // Minúsculas. Coordenadas normalizadas al recuadro real del glifo, que ya
            // incluye ascendentes/descendentes: en letras de caja baja (a, c, e, o…) el
            // recuadro es solo la altura-x; en las de asta alta (b, d, h, k, l, t) o cola
            // (g, j, p, q, y) el bucle/cola se ubica en la porción correspondiente.
            'a' -> listOf(
                listOf(
                    pt(0.74f, 0.3f), pt(0.46f, 0.06f), pt(0.18f, 0.3f),
                    pt(0.1f, 0.55f), pt(0.2f, 0.82f), pt(0.46f, 0.96f), pt(0.74f, 0.72f)
                ),
                listOf(pt(0.76f, 0.1f), pt(0.76f, 0.98f))
            )
            'b' -> listOf(
                listOf(pt(0.2f, 0.02f), pt(0.2f, 0.98f)),
                listOf(
                    pt(0.2f, 0.45f), pt(0.5f, 0.34f), pt(0.8f, 0.55f),
                    pt(0.78f, 0.8f), pt(0.5f, 0.98f), pt(0.2f, 0.9f)
                )
            )
            'c' -> listOf(
                listOf(
                    pt(0.82f, 0.25f), pt(0.55f, 0.05f), pt(0.25f, 0.18f),
                    pt(0.12f, 0.5f), pt(0.25f, 0.82f), pt(0.55f, 0.95f), pt(0.82f, 0.75f)
                )
            )
            'd' -> listOf(
                listOf(pt(0.8f, 0.02f), pt(0.8f, 0.98f)),
                listOf(
                    pt(0.8f, 0.45f), pt(0.5f, 0.34f), pt(0.2f, 0.55f),
                    pt(0.22f, 0.8f), pt(0.5f, 0.98f), pt(0.8f, 0.9f)
                )
            )
            'e' -> listOf(
                listOf(
                    pt(0.12f, 0.55f), pt(0.85f, 0.55f), pt(0.82f, 0.25f),
                    pt(0.5f, 0.05f), pt(0.2f, 0.2f), pt(0.1f, 0.5f),
                    pt(0.22f, 0.82f), pt(0.55f, 0.95f), pt(0.82f, 0.78f)
                )
            )
            'f' -> listOf(
                listOf(
                    pt(0.78f, 0.18f), pt(0.55f, 0.04f), pt(0.35f, 0.12f),
                    pt(0.32f, 0.4f), pt(0.32f, 0.98f)
                ),
                listOf(pt(0.12f, 0.4f), pt(0.7f, 0.4f))
            )
            'g' -> listOf(
                listOf(
                    pt(0.78f, 0.2f), pt(0.5f, 0.05f), pt(0.22f, 0.2f),
                    pt(0.15f, 0.4f), pt(0.28f, 0.58f), pt(0.6f, 0.6f), pt(0.78f, 0.45f)
                ),
                listOf(pt(0.78f, 0.06f), pt(0.78f, 0.72f), pt(0.58f, 0.95f), pt(0.28f, 0.88f))
            )
            'h' -> listOf(
                listOf(pt(0.2f, 0.02f), pt(0.2f, 0.98f)),
                listOf(
                    pt(0.2f, 0.5f), pt(0.45f, 0.38f), pt(0.75f, 0.45f),
                    pt(0.8f, 0.7f), pt(0.8f, 0.98f)
                )
            )
            'i' -> listOf(
                listOf(pt(0.5f, 0.02f), pt(0.5f, 0.1f)),
                listOf(pt(0.5f, 0.32f), pt(0.5f, 0.98f))
            )
            'j' -> listOf(
                listOf(pt(0.6f, 0.02f), pt(0.6f, 0.1f)),
                listOf(pt(0.6f, 0.32f), pt(0.6f, 0.8f), pt(0.45f, 0.96f), pt(0.2f, 0.88f))
            )
            'k' -> listOf(
                listOf(pt(0.2f, 0.02f), pt(0.2f, 0.98f)),
                listOf(pt(0.8f, 0.4f), pt(0.2f, 0.72f)),
                listOf(pt(0.4f, 0.6f), pt(0.82f, 0.98f))
            )
            'l' -> listOf(
                listOf(pt(0.5f, 0.02f), pt(0.5f, 0.98f))
            )
            'm' -> listOf(
                listOf(
                    pt(0.1f, 0.98f), pt(0.1f, 0.3f), pt(0.28f, 0.15f),
                    pt(0.45f, 0.3f), pt(0.45f, 0.98f)
                ),
                listOf(pt(0.45f, 0.3f), pt(0.63f, 0.15f), pt(0.82f, 0.3f), pt(0.82f, 0.98f))
            )
            'n' -> listOf(
                listOf(
                    pt(0.15f, 0.98f), pt(0.15f, 0.28f), pt(0.42f, 0.13f),
                    pt(0.78f, 0.3f), pt(0.78f, 0.98f)
                )
            )
            'o' -> listOf(
                listOf(
                    pt(0.5f, 0.05f), pt(0.2f, 0.2f), pt(0.1f, 0.5f),
                    pt(0.2f, 0.8f), pt(0.5f, 0.95f), pt(0.8f, 0.8f),
                    pt(0.9f, 0.5f), pt(0.8f, 0.2f), pt(0.5f, 0.05f)
                )
            )
            'p' -> listOf(
                listOf(pt(0.2f, 0.02f), pt(0.2f, 0.98f)),
                listOf(
                    pt(0.2f, 0.12f), pt(0.5f, 0.05f), pt(0.8f, 0.2f),
                    pt(0.8f, 0.45f), pt(0.5f, 0.62f), pt(0.2f, 0.6f)
                )
            )
            'q' -> listOf(
                listOf(pt(0.8f, 0.02f), pt(0.8f, 0.98f)),
                listOf(
                    pt(0.8f, 0.12f), pt(0.5f, 0.05f), pt(0.2f, 0.2f),
                    pt(0.2f, 0.45f), pt(0.5f, 0.62f), pt(0.8f, 0.55f)
                )
            )
            'r' -> listOf(
                listOf(pt(0.25f, 0.98f), pt(0.25f, 0.3f)),
                listOf(pt(0.25f, 0.45f), pt(0.45f, 0.2f), pt(0.75f, 0.25f))
            )
            's' -> listOf(
                listOf(
                    pt(0.78f, 0.22f), pt(0.5f, 0.08f), pt(0.25f, 0.2f),
                    pt(0.35f, 0.42f), pt(0.65f, 0.55f), pt(0.75f, 0.78f),
                    pt(0.5f, 0.95f), pt(0.2f, 0.82f)
                )
            )
            't' -> listOf(
                listOf(pt(0.45f, 0.02f), pt(0.45f, 0.82f), pt(0.65f, 0.96f), pt(0.82f, 0.88f)),
                listOf(pt(0.15f, 0.32f), pt(0.78f, 0.32f))
            )
            'u' -> listOf(
                listOf(
                    pt(0.18f, 0.05f), pt(0.18f, 0.7f), pt(0.35f, 0.92f),
                    pt(0.6f, 0.92f), pt(0.8f, 0.7f), pt(0.8f, 0.98f)
                )
            )
            'v' -> listOf(
                listOf(pt(0.12f, 0.05f), pt(0.5f, 0.95f), pt(0.88f, 0.05f))
            )
            'w' -> listOf(
                listOf(
                    pt(0.08f, 0.05f), pt(0.28f, 0.95f), pt(0.5f, 0.4f),
                    pt(0.72f, 0.95f), pt(0.92f, 0.05f)
                )
            )
            'x' -> listOf(
                listOf(pt(0.15f, 0.05f), pt(0.85f, 0.95f)),
                listOf(pt(0.85f, 0.05f), pt(0.15f, 0.95f))
            )
            'y' -> listOf(
                listOf(pt(0.15f, 0.05f), pt(0.48f, 0.55f)),
                listOf(pt(0.85f, 0.05f), pt(0.2f, 0.98f))
            )
            'z' -> listOf(
                listOf(pt(0.15f, 0.1f), pt(0.85f, 0.1f), pt(0.2f, 0.9f), pt(0.85f, 0.9f))
            )
            else -> null
        }
    }

    private fun prepareFillBitmap(w: Int, h: Int) {
        fillBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        fillCanvas = Canvas(fillBitmap!!)
        // Llenar el bitmap con fondo blanco para que sea visible
        fillCanvas!!.drawColor(Color.WHITE, PorterDuff.Mode.SRC)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        fillBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        if (completed && !coloringEnabled) {
            // Trazado terminado (aún sin "Dibujar"): mostrar SOLO el número completo
            // en un único trazo verde y limpio, sin los puntos guía encima, para que
            // no se vea un número duplicado.
            canvas.drawPath(tracePath, traceDoneGlowPaint)
            canvas.drawPath(tracePath, traceDonePaint)
            return
        }

        if (!coloringEnabled) {
            // Trazo del niño en curso: halo suave debajo + línea viva encima.
            canvas.drawPath(tracePath, traceGlowPaint)
            canvas.drawPath(tracePath, tracePaint)
        }

        if (completed && coloringEnabled) {
            canvas.drawPath(path, outlinePaint)
        }

        if (!coloringEnabled) {
            // Flechas que indican la dirección del trazo entre puntos consecutivos
            // todavía no recorridos, para guiar al niño hacia dónde continuar.
            drawDirectionArrows(canvas)

            for (i in points.indices) {
                val p = points[i]
                val r = if (visited[i]) 26f else 20f
                val paint = if (visited[i]) visitedPaint else pointPaint
                canvas.drawCircle(p.x, p.y, r, paint)
                if (!visited[i]) canvas.drawCircle(p.x, p.y, r, pointRingPaint)
            }

            if (activeIndex in points.indices && !visited[activeIndex]) {
                val p = points[activeIndex]
                activePaint.alpha = blinkAlpha
                canvas.drawCircle(p.x, p.y, 30f, activePaint)
                // Anillo pulsante exterior para que el punto activo destaque más.
                activeRingPaint.alpha = blinkAlpha
                canvas.drawCircle(p.x, p.y, 46f, activeRingPaint)
            }
        }
    }

    /**
     * Dibuja pequeñas flechas triangulares sobre el camino guía, apuntando del punto
     * activo hacia el siguiente, para sugerir la dirección correcta del trazo.
     */
    private fun drawDirectionArrows(canvas: Canvas) {
        if (activeIndex !in points.indices) return
        // Mostrar una flecha por cada uno de los próximos pocos segmentos sin recorrer.
        val maxArrows = 3
        var drawn = 0
        var i = activeIndex
        while (i < points.size - 1 && drawn < maxArrows) {
            val a = points[i]
            val b = points[i + 1]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (len > 1f) {
                val ux = dx / len
                val uy = dy / len
                // Colocar la punta de la flecha en el punto medio del segmento.
                val mx = (a.x + b.x) / 2f
                val my = (a.y + b.y) / 2f
                val size = 22f
                val tipX = mx + ux * size / 2f
                val tipY = my + uy * size / 2f
                // Vector perpendicular para las alas del triángulo.
                val px = -uy
                val py = ux
                val baseX = mx - ux * size / 2f
                val baseY = my - uy * size / 2f
                val arrow = Path().apply {
                    moveTo(tipX, tipY)
                    lineTo(baseX + px * size * 0.5f, baseY + py * size * 0.5f)
                    lineTo(baseX - px * size * 0.5f, baseY - py * size * 0.5f)
                    close()
                }
                arrowPaint.alpha = 200 - drawn * 50
                canvas.drawPath(arrow, arrowPaint)
                drawn++
            }
            i++
        }
    }

    private fun startBlink() {
        val animator = ValueAnimator.ofInt(50, 255)
        animator.duration = 700
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener {
            blinkAlpha = it.animatedValue as Int
            invalidate()
        }
        animator.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (coloringEnabled) {
                    lastX = x
                    lastY = y
                    isDrawing = true
                    // Pintar un punto al tocar para que un toque simple deje marca.
                    if (fillCanvas != null && fillBitmap != null) {
                        fillCanvas!!.drawCircle(x, y, linePaint.strokeWidth / 2f, linePaint)
                        invalidate()
                    }
                    return true
                }
                // Modo trazado: un toque cerca del punto activo ya lo registra,
                // para que el primer punto no dependa de mover el dedo. Reiniciar el
                // tramo: este toque arranca un nuevo deslizamiento, no continúa el previo.
                traceLastX = -1f
                traceLastY = -1f
                tryMagnetizeTrace(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (coloringEnabled && isDrawing) {
                    if (fillCanvas != null && fillBitmap != null) {
                        // Dibujar línea continua desde el punto anterior
                        if (lastX >= 0 && lastY >= 0) {
                            fillCanvas!!.drawLine(lastX, lastY, x, y, linePaint)
                        } else {
                            // Si es el primer punto, dibujar un círculo pequeño
                            fillCanvas!!.drawCircle(x, y, 22f, linePaint)
                        }
                        lastX = x
                        lastY = y
                        invalidate()
                    }
                    return true
                } else if (!coloringEnabled) {
                    // Modo trazado: magnetizar el dedo a los puntos guía en orden.
                    tryMagnetizeTrace(x, y)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDrawing) {
                    lastX = -1f
                    lastY = -1f
                    isDrawing = false
                }
                // Cerrar el tramo de trazado: al levantar el dedo no debe medirse un
                // segmento contra la siguiente posición (que sería un salto distinto).
                traceLastX = -1f
                traceLastY = -1f
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Avanza el trazado **punto por punto y en estricto orden secuencial**: solo marca
     * como visitado el SIGUIENTE punto pendiente ([activeIndex]) si el dedo —o el tramo
     * que recorrió desde la última posición— pasa lo bastante cerca de él. El bucle
     * permite encadenar varios puntos seguidos cuando un mismo deslizamiento rápido
     * barre varios, pero siempre en orden, de modo que el niño debe recorrer la línea.
     *
     * Antes se buscaba el punto globalmente más cercano y se rellenaba de golpe todo el
     * rango hasta él; en glifos con trazos que se cruzan (p. ej. el **4**) el dedo se
     * acercaba a un punto del otro trazo y el número aparecía completo de inmediato.
     * Se invoca tanto en ACTION_DOWN (para que el primer toque inicie el trazo) como en
     * ACTION_MOVE. Medir contra el tramo [traceLastX]→(x,y) evita saltarse puntos al
     * deslizar rápido sin perder el orden.
     */
    private fun tryMagnetizeTrace(x: Float, y: Float) {
        if (completed) return
        while (activeIndex in points.indices) {
            val p = points[activeIndex]
            val d = if (traceLastX >= 0f && traceLastY >= 0f) {
                distanceToSegment(p.x, p.y, traceLastX, traceLastY, x, y)
            } else {
                hypot((p.x - x).toDouble(), (p.y - y).toDouble()).toFloat()
            }
            if (d <= magnetRadius) {
                markVisited(activeIndex)
            } else {
                break
            }
        }
        traceLastX = x
        traceLastY = y
    }

    /** Distancia mínima del punto (px,py) al segmento [a,b]. */
    private fun distanceToSegment(
        px: Float, py: Float,
        ax: Float, ay: Float,
        bx: Float, by: Float
    ): Float {
        val abx = bx - ax
        val aby = by - ay
        val ab2 = abx * abx + aby * aby
        val t = if (ab2 <= 0f) {
            0f
        } else {
            (((px - ax) * abx + (py - ay) * aby) / ab2).coerceIn(0f, 1f)
        }
        val cx = ax + abx * t
        val cy = ay + aby * t
        return hypot((px - cx).toDouble(), (py - cy).toDouble()).toFloat()
    }

    private fun markVisited(idx: Int) {
        if (idx !in points.indices) return
        if (completed) return        // completion fires once
        if (visited[idx]) return     // idempotent back-fill
        visited[idx] = true
        // Levantar la pluma (moveTo) al iniciar un nuevo trazo del glifo; en cualquier
        // otro caso continuar la línea (lineTo). Así no se dibujan líneas azules que
        // crucen entre trazos sueltos de la letra (p. ej. las diagonales de la A).
        if (tracePath.isEmpty || idx in contourStarts) {
            tracePath.moveTo(points[idx].x, points[idx].y)
        } else {
            tracePath.lineTo(points[idx].x, points[idx].y)
        }
        activeIndex = visited.indexOfFirst { !it }
        if (activeIndex == -1) activeIndex = points.size
        invalidate()
        if (visited.all { it }) {
            onTracingComplete()
        }
    }

    private fun onTracingComplete() {
        completed = true
        tts?.speak(getCompletionPhrase(), TextToSpeech.QUEUE_FLUSH, null, "trace_done")
        val anim = ValueAnimator.ofFloat(1f, 1.06f, 1f)
        anim.duration = 600
        anim.addUpdateListener {
            val v = it.animatedValue as Float
            scaleX = v
            scaleY = v
        }
        anim.start()
        onComplete?.invoke()
    }

    private fun getCompletionPhrase(): String {
        return if (targetChar.isDigit()) {
            val numberWords = mapOf(
                '0' to "¡Cero!",
                '1' to "¡Uno!",
                '2' to "¡Dos!",
                '3' to "¡Tres!",
                '4' to "¡Cuatro!",
                '5' to "¡Cinco!",
                '6' to "¡Seis!",
                '7' to "¡Siete!",
                '8' to "¡Ocho!",
                '9' to "¡Nueve!"
            )
            numberWords[targetChar] ?: "¡Número $targetChar!"
        } else {
            "¡Letra $targetChar!"
        }
    }

    private fun isInsidePath(x: Float, y: Float): Boolean {
        return pathRegion.contains(x.toInt(), y.toInt())
    }

    fun enableColoringMode() {
        // En pizarra libre, la vista puede no estar medida todavía (completed=false).
        // Igual habilitamos el coloreado; onSizeChanged preparará el lienzo.
        if (!completed && !freeMode) return
        coloringEnabled = true
        confettiAnimator?.cancel()
        confettiParticles.clear()
        invalidate()
    }

    fun setFillColor(color: Int) {
        fillPaint.color = color
        fillPaint.alpha = 180
        linePaint.color = color
        linePaint.alpha = 180
    }

    fun clearColoring() {
        fillCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
    }

    private fun updateConfetti() {
        val iterator = confettiParticles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.x += particle.vx
            particle.y += particle.vy
            particle.vy += 0.2f
            if (particle.y > height + particle.radius) {
                particle.y = -particle.radius
                particle.x = (Math.random() * width).toFloat()
            }
            if (particle.x < -particle.radius) particle.x = width + particle.radius
            if (particle.x > width + particle.radius) particle.x = -particle.radius
        }
    }

    private fun drawConfetti(canvas: Canvas) {
        confettiParticles.forEach {
            confettiPaint.color = it.color
            canvas.drawCircle(it.x, it.y, it.radius, confettiPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        tts?.shutdown()
        confettiAnimator?.cancel()
    }

    private data class ConfettiParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var radius: Float,
        var color: Int
    )
}
