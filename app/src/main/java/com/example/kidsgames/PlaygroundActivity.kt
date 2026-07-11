package com.example.kidsgames

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PlaygroundActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)

        val moduleName = intent.getStringExtra("moduleName") ?: "Módulo"
        val playgroundTitle = findViewById<TextView>(R.id.playgroundTitle)
        val playgroundInstructions = findViewById<TextView>(R.id.playgroundInstructions)

        if (moduleName == "Pizarra Libre") {
            playgroundTitle.text = "🎨 Pizarra Libre"
            playgroundInstructions.text = "¡Elige un color y dibuja lo que quieras!"
        } else {
            playgroundTitle.text = moduleName
            playgroundInstructions.text = "Selecciona Números o Letras para practicar trazado gigante y presiona Dibujar cuando completes el trazado."
        }

        val numbersButton = findViewById<View>(R.id.btnNumbers)
        val upperButton = findViewById<View>(R.id.btnUpper)
        val lowerButton = findViewById<View>(R.id.btnLower)
        val grid = findViewById<GridLayout>(R.id.selectionGrid)
        val categoriesContainer = findViewById<LinearLayout>(R.id.categoriesContainer)
        val traceContainer = findViewById<LinearLayout>(R.id.traceContainer)
        val traceContent = findViewById<FrameLayout>(R.id.traceContent)
        val selectionPanel = findViewById<LinearLayout>(R.id.selectionPanel)
        val backButton = findViewById<Button>(R.id.btnBackFromTrace)
        val homeButton = findViewById<Button>(R.id.btnHome)
        val btnHomeFromTrace = findViewById<Button>(R.id.btnHomeFromTrace)
        val btnDraw = findViewById<Button>(R.id.btnDraw)
        val colorToolsPanel = findViewById<LinearLayout>(R.id.colorToolsPanel)
        val traceCharacterLabel = findViewById<TextView>(R.id.traceCharacterLabel)
        val btnColorRed = findViewById<View>(R.id.btnColorRed)
        val btnColorBlue = findViewById<View>(R.id.btnColorBlue)
        val btnColorYellow = findViewById<View>(R.id.btnColorYellow)
        val btnColorGreen = findViewById<View>(R.id.btnColorGreen)
        val btnColorPurple = findViewById<View>(R.id.btnColorPurple)
        val btnColorBrown = findViewById<View>(R.id.btnColorBrown)
        val btnColorPink = findViewById<View>(R.id.btnColorPink)
        val btnColorBlack = findViewById<View>(R.id.btnColorBlack)
        val btnColorWhite = findViewById<View>(R.id.btnColorWhite)
        val btnRevertColoring = findViewById<ImageButton>(R.id.btnRevertColoring)

        var currentTraceView: TraceView? = null

        fun resetTraceControls() {
            btnDraw.visibility = View.GONE
            colorToolsPanel.visibility = View.GONE
        }

        fun showSelection() {
            traceContainer.visibility = View.GONE
            selectionPanel.visibility = View.VISIBLE
            categoriesContainer.visibility = View.VISIBLE
            grid.removeAllViews()
            traceContent.removeAllViews()
            resetTraceControls()
        }

        fun showTraceForChar(ch: Char) {
            selectionPanel.visibility = View.GONE
            traceContainer.visibility = View.VISIBLE
            traceContent.removeAllViews()
            resetTraceControls()
            traceCharacterLabel.text = ch.toString()
            val traceView = TraceView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setTargetChar(ch)
                setOnCompleteListener {
                    btnDraw.visibility = View.VISIBLE
                    Toast.makeText(
                        this@PlaygroundActivity,
                        "¡Trazado completado! Presiona Dibujar para comenzar a colorear.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            currentTraceView = traceView
            traceContent.addView(traceView)
        }

        backButton.setOnClickListener { showSelection() }

        btnHomeFromTrace.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        homeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        btnDraw.setOnClickListener {
            currentTraceView?.enableColoringMode()
            btnDraw.visibility = View.GONE
            colorToolsPanel.visibility = View.VISIBLE
        }

        fun setBrushColor(color: Int) {
            currentTraceView?.setFillColor(color)
        }

        btnColorRed.setOnClickListener { setBrushColor(resources.getColor(android.R.color.holo_red_dark, null)) }
        btnColorBlue.setOnClickListener { setBrushColor(resources.getColor(android.R.color.holo_blue_dark, null)) }
        btnColorYellow.setOnClickListener { setBrushColor(resources.getColor(android.R.color.holo_orange_light, null)) }
        btnColorGreen.setOnClickListener { setBrushColor(resources.getColor(android.R.color.holo_green_dark, null)) }
        btnColorPurple.setOnClickListener { setBrushColor(resources.getColor(android.R.color.holo_purple, null)) }
        btnColorBrown.setOnClickListener { setBrushColor(resources.getColor(R.color.brown, null)) }
        btnColorPink.setOnClickListener { setBrushColor(resources.getColor(R.color.pink, null)) }
        btnColorBlack.setOnClickListener { setBrushColor(resources.getColor(R.color.black, null)) }
        btnColorWhite.setOnClickListener { setBrushColor(resources.getColor(R.color.white, null)) }

        btnRevertColoring.setOnClickListener {
            currentTraceView?.clearColoring()
            currentTraceView?.enableColoringMode()
        }

        // Paleta rotativa para dar a cada ficha un color vivo distinto.
        val tilePalette = intArrayOf(
            R.color.tile_1, R.color.tile_2, R.color.tile_3,
            R.color.tile_4, R.color.tile_5, R.color.tile_6
        )

        fun addCharTile(label: String, paletteIndex: Int, onClick: () -> Unit) {
            val density = resources.displayMetrics.density
            val margin = (6 * density).toInt()
            val bgColor = ContextCompat.getColor(this, tilePalette[paletteIndex % tilePalette.size])
            val tile = Button(this).apply {
                text = label
                isAllCaps = false
                textSize = 26f
                setTextColor(android.graphics.Color.WHITE)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = ContextCompat.getDrawable(context, R.drawable.grid_char_button_bg)
                backgroundTintList = android.content.res.ColorStateList.valueOf(bgColor)
                stateListAnimator = null
                elevation = 5 * density
                setPadding(0, 0, 0, 0)
                minWidth = 0
                minHeight = 0
                minimumWidth = 0
                minimumHeight = 0
            }
            tile.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = (64 * density).toInt()
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(margin, margin, margin, margin)
            }
            tile.setOnClickListener { onClick() }
            grid.addView(tile)
        }

        numbersButton.setOnClickListener {
            categoriesContainer.visibility = View.GONE
            grid.removeAllViews()
            grid.columnCount = 5
            for (i in 1..20) {
                addCharTile(i.toString(), i - 1) { showTraceForChar(i.toString()[0]) }
            }
        }

        upperButton.setOnClickListener {
            categoriesContainer.visibility = View.GONE
            grid.removeAllViews()
            grid.columnCount = 6
            for ((idx, c) in ('A'..'Z').withIndex()) {
                addCharTile(c.toString(), idx) { showTraceForChar(c) }
            }
        }

        lowerButton.setOnClickListener {
            categoriesContainer.visibility = View.GONE
            grid.removeAllViews()
            grid.columnCount = 6
            for ((idx, c) in ('a'..'z').withIndex()) {
                addCharTile(c.toString(), idx) { showTraceForChar(c) }
            }
        }

        // Inicializar según el módulo seleccionado
        if (moduleName == "Pizarra Libre") {
            // Mostrar directamente el modo de dibujo libre sin pasos intermedios
            selectionPanel.visibility = View.GONE
            traceContainer.visibility = View.VISIBLE
            traceContent.removeAllViews()
            traceCharacterLabel.visibility = View.GONE
            
            val freeTraceView = TraceView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setFreeMode(true)
            }
            currentTraceView = freeTraceView
            traceContent.addView(freeTraceView)
            
            // Activar directamente el modo de coloreo con colores visibles
            currentTraceView?.enableColoringMode()
            btnDraw.visibility = View.GONE
            colorToolsPanel.visibility = View.VISIBLE
            
            // Ocultar el botón de volver a selección en modo Pizarra Libre
            backButton.visibility = View.GONE

            btnHomeFromTrace.text = "🏠  Ir al inicio"
            btnHomeFromTrace.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
        } else {
            // Para otros módulos, mostrar la selección de números/letras
            showSelection()
        }
    }
}
