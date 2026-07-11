package com.example.kidsgames

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class ModuleAdapter(
    private val items: List<ModuleItem>,
    private val onClick: (ModuleItem) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.module_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.icon.text = item.icon
        val color = ContextCompat.getColor(holder.card.context, item.colorRes)
        holder.card.setCardBackgroundColor(ColorStateList.valueOf(color))
        holder.card.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.moduleTitle)
        val subtitle: TextView = itemView.findViewById(R.id.moduleSubtitle)
        val icon: TextView = itemView.findViewById(R.id.moduleIcon)
        val card: MaterialCardView = itemView.findViewById(R.id.moduleCard)
    }
}
