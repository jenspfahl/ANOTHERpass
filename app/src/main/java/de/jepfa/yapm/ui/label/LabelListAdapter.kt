package de.jepfa.yapm.ui.label

import android.app.ActionBar
import android.content.Context
import android.view.Gravity
import android.view.View
import de.jepfa.yapm.util.createLabelChip
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.TextView
import de.jepfa.yapm.R

import java.util.*

class LabelListAdapter(context: Context,
                       labels: List<Label>
) : ArrayAdapter<Label>(
    context, R.layout.content_dynamic_labels_list, labels
) {
    private var labels: List<Label>
    private val filteredLabels: MutableList<Label> = ArrayList()
    private var labelFilter: LabelFilter? = null

    init {
        this.labels = labels
    }

    override fun getCount(): Int {
        return filteredLabels.size
    }

    @Synchronized
    override fun getFilter(): Filter {
        if (labelFilter == null) {
            labelFilter = LabelFilter(this, labels)
        }
        return labelFilter!!
    }

    override fun getItem(position: Int): Label? {
        return filteredLabels[position]
    }

    override fun getItemId(position: Int): Long {
        return filteredLabels[position].labelId?.toLong()?:0
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val label = filteredLabels[position]

        val container = LinearLayout(context)
        parent.textAlignment = View.TEXT_ALIGNMENT_CENTER
        val chip = createLabelChip(label, false, context)
        chip.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        chip.isClickable = false
        chip.isFocusable = false
        container.addView(chip)
        container.isClickable = false
        container.isFocusable = false
        container.gravity = Gravity.CENTER
        return container

    }

    private inner class LabelFilter(
        var labelListAdapter: LabelListAdapter,
        var originalList: List<Label>
    ) : Filter() {

        var filteredList: MutableList<Label> = ArrayList()

        override fun performFiltering(constraint: CharSequence): FilterResults {
            filteredList.clear()
            val results = FilterResults()
            if (constraint == null || constraint.length == 0) {
                filteredList.addAll(originalList)
            } else {
                val filterPattern =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                for (label in originalList) {
                    if (label.name.lowercase(Locale.getDefault()).contains(filterPattern)) {
                        filteredList.add(label)
                    }
                }
            }
            results.values = filteredList
            results.count = filteredList.size
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            labelListAdapter.filteredLabels.clear()
            if (results.values != null && results.values is MutableList<*>) {
                labelListAdapter.filteredLabels.addAll((results.values as MutableList<Label>).sortedBy { it.name })
            }
            labelListAdapter.notifyDataSetChanged()
        }

    }

}