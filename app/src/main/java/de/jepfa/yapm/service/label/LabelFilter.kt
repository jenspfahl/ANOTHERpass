package de.jepfa.yapm.service.label

import de.jepfa.yapm.ui.label.Label
import java.util.concurrent.ConcurrentHashMap

object LabelFilter {
    const val WITH_NO_LABELS_ID = -1

    private val filterByLabelNames: MutableMap<String, Label> = ConcurrentHashMap(32)
    private var filterByNoLabels = false


    fun setFilterFor(label: Label) {
        if (isAssociatedWithNoLabels(label)) {
            filterByNoLabels = true
        }
        else {
            filterByLabelNames.put(label.name, label)
        }
    }

    fun unsetFilterFor(label: Label) {
        if (isAssociatedWithNoLabels(label)) {
            filterByNoLabels = false
        }
        else {
            filterByLabelNames.remove(label.name)
        }
    }

    fun unsetAllFilters() {
        filterByNoLabels = false
        filterByLabelNames.clear()
    }

    fun hasFilters(): Boolean {
        return !filterByLabelNames.none() || filterByNoLabels == true
    }

    fun isFilterFor(labels: Collection<Label>): Boolean {
        if (!hasFilters()) {
            return true
        }
        return (labels.isEmpty() && filterByNoLabels) || !labels.none { isFilterFor(it) }
    }

    fun isFilterFor(label: Label): Boolean {
        return filterByLabelNames.containsKey(label.name) || (isAssociatedWithNoLabels(label) && filterByNoLabels)
    }

    private fun isAssociatedWithNoLabels(label: Label) =
        label.labelId == WITH_NO_LABELS_ID

}