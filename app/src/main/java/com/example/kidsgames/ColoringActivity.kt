package com.example.kidsgames

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

/**
 * Módulo 1: ¡A Colorear!
 *
 * Pantalla principal de coloreo. Muestra una plantilla de contorno en un lienzo
 * central ([ColoringView]), una paleta horizontal debajo del lienzo con botones
 * circulares gigantes y una barra superior con zoom, guardar, reset y candado
 * anti-errores.
 */
class ColoringActivity : AppCompatActivity() {

    private lateinit var coloringView: ColoringView
    private lateinit var btnLock: ImageButton
    private lateinit var btnPanUp: ImageButton
    private lateinit var btnPanDown: ImageButton
    private lateinit var btnPanLeft: ImageButton
    private lateinit var btnPanRight: ImageButton

    /** Chips del selector de plantillas, para resaltar el seleccionado. */
    private val templateChips = mutableListOf<Button>()

    private val unlockHandler = Handler(Looper.getMainLooper())
    private var unlockRunnable: Runnable? = null

    /** Plantillas disponibles: nombre visible + drawable de contorno. */
    private val templates = listOf(
        "Esqueleto" to R.drawable.template_esqueleto,
        "Bob" to R.drawable.template_bob,
        "Pocoyo" to R.drawable.template_pocoyo,
        "Tra 1" to R.drawable.template_tra1,
        "Tra 2" to R.drawable.template_tra2,
        "Estrella" to R.drawable.template_star,
        "Corazón" to R.drawable.template_heart,
        "Casa" to R.drawable.template_house,
        "Flor" to R.drawable.template_flower
    )

    /** Colores de la paleta: etiqueta + color. */
    private val palette = listOf(
        "Rojo" to android.R.color.holo_red_dark,
        "Naranja" to android.R.color.holo_orange_dark,
        "Amarillo" to android.R.color.holo_orange_light,
        "Verde" to android.R.color.holo_green_dark,
        "Azul" to android.R.color.holo_blue_dark,
        "Morado" to android.R.color.holo_purple,
        "Rosa" to R.color.pink,
        "Café" to R.color.brown,
        "Blanco" to R.color.white,
        "Negro" to R.color.black
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coloring)

        coloringView = findViewById(R.id.coloringView)
        btnLock = findViewById(R.id.btnLock)
        btnPanUp = findViewById(R.id.btnPanUp)
        btnPanDown = findViewById(R.id.btnPanDown)
        btnPanLeft = findViewById(R.id.btnPanLeft)
        btnPanRight = findViewById(R.id.btnPanRight)

        buildTemplatePicker()
        buildColorPalette()

        // Plantilla inicial.
        coloringView.setTemplate(templates.first().second)
        coloringView.setBrushColor(ContextCompat.getColor(this, palette.first().second))
        selectTemplateChip(0)

        setupToolbar()
        setupPanButtons()

        findViewById<Button>(R.id.btnHomeColoring).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }

    private fun buildTemplatePicker() {
        val row = findViewById<LinearLayout>(R.id.templateRow)
        templateChips.clear()
        templates.forEachIndexed { index, (name, resId) ->
            val chip = Button(this).apply {
                text = name
                isAllCaps = false
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = ContextCompat.getDrawable(
                    this@ColoringActivity, R.drawable.template_chip_bg
                )
                stateListAnimator = null
                minWidth = 0
                minimumWidth = 0
                setPadding(dp(20), dp(8), dp(20), dp(8))
                setOnClickListener {
                    if (coloringView.isLocked()) return@setOnClickListener
                    coloringView.setTemplate(resId)
                    selectTemplateChip(index)
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(10) }
            row.addView(chip, lp)
            templateChips.add(chip)
        }
    }

    /** Resalta el chip de plantilla seleccionado y atenúa los demás. */
    private fun selectTemplateChip(selectedIndex: Int) {
        val selectedText = ContextCompat.getColor(this, R.color.text_on_card)
        val idleText = ContextCompat.getColor(this, R.color.kids_subtitle)
        templateChips.forEachIndexed { index, chip ->
            val isSelected = index == selectedIndex
            chip.isSelected = isSelected
            chip.setTextColor(if (isSelected) selectedText else idleText)
        }
    }

    private fun buildColorPalette() {
        val container = findViewById<LinearLayout>(R.id.colorPalette)
        val pencilWidth = dp(40)
        val pencilHeight = dp(84)
        palette.forEach { (label, colorRes) ->
            val color = ContextCompat.getColor(this, colorRes)
            val swatch = View(this).apply {
                background = pencilBg(color)
                contentDescription = label
                setOnClickListener {
                    if (coloringView.isLocked()) return@setOnClickListener
                    coloringView.setBrushColor(color)
                    Toast.makeText(this@ColoringActivity, label, Toast.LENGTH_SHORT).show()
                }
            }
            val lp = LinearLayout.LayoutParams(pencilWidth, pencilHeight).apply {
                marginEnd = dp(10)
            }
            container.addView(swatch, lp)
        }

        // Borrar todo: mismo estilo de "lápiz" blanco que la paleta, con ícono de bote de basura.
        // Pintar de blanco ya cubre el borrado puntual, así que este botón limpia todo el lienzo.
        val clearAll = ImageButton(this).apply {
            background = pencilBg(ContextCompat.getColor(this@ColoringActivity, R.color.white))
            setImageResource(R.drawable.ic_trash)
            setColorFilter(ContextCompat.getColor(this@ColoringActivity, R.color.black))
            setPadding(dp(8), dp(28), dp(8), dp(28))
            contentDescription = "Borrar todo el coloreado"
            setOnClickListener {
                if (coloringView.isLocked()) return@setOnClickListener
                coloringView.reset()
                Toast.makeText(this@ColoringActivity, "Borrar todo", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(clearAll, LinearLayout.LayoutParams(pencilWidth, pencilHeight))
    }

    /** Fondo rectangular redondeado tipo "lápiz de color" para la paleta. */
    private fun pencilBg(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        cornerRadius = dp(12).toFloat()
        setStroke(dp(2), 0x33000000)
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btnZoomIn).setOnClickListener { coloringView.zoomIn() }
        findViewById<ImageButton>(R.id.btnZoomOut).setOnClickListener { coloringView.zoomOut() }
        findViewById<ImageButton>(R.id.btnReset).setOnClickListener { coloringView.reset() }
        findViewById<ImageButton>(R.id.btnSave).setOnClickListener { saveCreation() }
        setupLock()
    }

    /**
     * Flechas triangulares para mover el lienzo ampliado por zoom. Cada una
     * solo se muestra mientras todavía haya margen para desplazarse en esa
     * dirección; al llegar al borde del marco, la flecha correspondiente
     * desaparece.
     */
    private fun setupPanButtons() {
        btnPanUp.setOnClickListener { coloringView.panUp() }
        btnPanDown.setOnClickListener { coloringView.panDown() }
        btnPanLeft.setOnClickListener { coloringView.panLeft() }
        btnPanRight.setOnClickListener { coloringView.panRight() }
        coloringView.onViewportChanged = { updatePanButtonsVisibility() }
        updatePanButtonsVisibility()
    }

    private fun updatePanButtonsVisibility() {
        val locked = coloringView.isLocked()
        btnPanUp.visibility = if (!locked && coloringView.canPanUp()) View.VISIBLE else View.GONE
        btnPanDown.visibility = if (!locked && coloringView.canPanDown()) View.VISIBLE else View.GONE
        btnPanLeft.visibility = if (!locked && coloringView.canPanLeft()) View.VISIBLE else View.GONE
        btnPanRight.visibility = if (!locked && coloringView.canPanRight()) View.VISIBLE else View.GONE
    }

    /**
     * Candado anti-errores. Al activarse bloquea zoom, movimiento, reset y
     * guardar. Para desbloquear hay que MANTENER presionado el botón 3 segundos.
     */
    private fun setupLock() {
        btnLock.setOnClickListener {
            if (!coloringView.isLocked()) {
                coloringView.setLocked(true)
                btnLock.setImageResource(R.drawable.ic_lock_closed)
                updatePanButtonsVisibility()
                Toast.makeText(
                    this,
                    "Candado activado. Mantén presionado el candado 3 segundos para desbloquear.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        btnLock.setOnTouchListener { _, event ->
            if (!coloringView.isLocked()) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    unlockRunnable = Runnable {
                        coloringView.setLocked(false)
                        btnLock.setImageResource(R.drawable.ic_lock_open)
                        updatePanButtonsVisibility()
                        Toast.makeText(this, "¡Desbloqueado!", Toast.LENGTH_SHORT).show()
                    }
                    unlockHandler.postDelayed(unlockRunnable!!, 3000)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    unlockRunnable?.let { unlockHandler.removeCallbacks(it) }
                    true
                }
                else -> false
            }
        }
    }

    /** Efecto "Nevera Virtual": guarda el dibujo como JPEG y abre Mis Creaciones. */
    private fun saveCreation() {
        if (coloringView.isLocked()) return
        val bitmap = coloringView.exportBitmap()
        try {
            val dir = creationsDir()
            val file = File(dir, "creacion_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            Toast.makeText(this, "¡Guardado en Mis Creaciones!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, CreationsActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo guardar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** Carpeta de creaciones dentro del almacenamiento de la app. */
    private fun creationsDir(): File {
        val dir = File(filesDir, "DibujosParaColorear")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        unlockRunnable?.let { unlockHandler.removeCallbacks(it) }
    }
}
