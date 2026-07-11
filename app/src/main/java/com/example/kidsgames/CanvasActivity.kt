package com.example.kidsgames

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CanvasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_canvas)

        val startButton = findViewById<Button>(R.id.startCanvasButton)
        startButton.setOnClickListener {
            val intent = Intent(this, PlaygroundActivity::class.java)
            intent.putExtra("moduleName", "Pizarra Libre")
            startActivity(intent)
        }
    }
}
