package de.jepfa.yapm.ui

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.widget.LinearLayout

class ViewRecyclerViewAdapter(private val context: Context, private val items: List<View>) :
    RecyclerView.Adapter<ViewRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LinearLayout(
                context
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.itemView as ViewGroup).addView(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)

}