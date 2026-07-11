package com.example.kidsgames

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Pantalla "Mis Creaciones" — la **Nevera Virtual**.
 *
 * Muestra los dibujos guardados por el módulo de coloreo como fotos sujetas con
 * imanes a la puerta de un refrigerador. Lee los JPEG de la carpeta
 * `DibujosParaColorear` dentro del almacenamiento de la app.
 */
class CreationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creations)

        findViewById<Button>(R.id.btnHomeCreations).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }

        renderCreations()
    }

    private fun renderCreations() {
        val grid = findViewById<GridLayout>(R.id.creationsGrid)
        val emptyState = findViewById<TextView>(R.id.emptyState)
        grid.removeAllViews()

        val dir = File(filesDir, "DibujosParaColorear")
        val files = dir.listFiles { f -> f.extension.equals("jpg", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        if (files.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            return
        }
        emptyState.visibility = View.GONE

        val cellSize = (resources.displayMetrics.widthPixels / 2) - dp(32)
        files.forEach { file ->
            val frame = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@CreationsActivity, R.drawable.magnet_frame_bg)
                setPadding(dp(8), dp(8), dp(8), dp(8))
            }
            val image = ImageView(this).apply {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                setImageBitmap(bmp)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            frame.addView(
                image,
                LinearLayout.LayoutParams(cellSize, cellSize)
            )

            val lp = GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.WRAP_CONTENT
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(dp(8), dp(8), dp(8), dp(8))
            }
            grid.addView(frame, lp)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
