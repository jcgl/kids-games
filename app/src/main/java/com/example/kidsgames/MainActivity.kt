package com.example.kidsgames

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val moduleList = listOf(
            ModuleItem("Colorear", "Dibuja y guarda tus creaciones", "🎨", R.color.card_coral, ColoringActivity::class.java),
            ModuleItem("Une los Puntos", "Practica letras y números", "✏️", R.color.card_teal, PlaygroundActivity::class.java),
            ModuleItem("Pizarra Libre", "Dibuja libre, traza y colorea", "🖌️", R.color.card_violet, PlaygroundActivity::class.java)
        )

        val recyclerView = findViewById<RecyclerView>(R.id.moduleRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 1)
        recyclerView.adapter = ModuleAdapter(moduleList) { item ->
            val intent = Intent(this, item.activityClass)
            intent.putExtra("moduleName", item.title)
            startActivity(intent)
        }
    }
}

data class ModuleItem(
    val title: String,
    val subtitle: String,
    val icon: String,
    val colorRes: Int,
    val activityClass: Class<*>
)
