package com.example.kidsgames

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class TracingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracing)

        val startButton = findViewById<Button>(R.id.startTracingButton)
        startButton.setOnClickListener {
            val intent = Intent(this, PlaygroundActivity::class.java)
            intent.putExtra("moduleName", "Une los Puntos")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnHomeTracing).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
        }
    }
}
