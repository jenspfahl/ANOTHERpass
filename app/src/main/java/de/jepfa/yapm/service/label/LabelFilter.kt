package de.jepfa.yapm.service.label

import de.jepfa.yapm.ui.label.Label
import java.util.concurrent.ConcurrentHashMap

object LabelFilter {
    private val filterByLabels: MutableMap<String, Label> = ConcurrentHashMap(32)


    fun setFilterFor(label: Label): Label? {
        return filterByLabels.put(label.name, label)
    }

    fun unsetFilterFor(label: Label): Label? {
        return filterByLabels.remove(label.name)
    }

    fun unsetAllFilters() {
        filterByLabels.clear()
    }

    fun hasFilters(): Boolean {
        return !filterByLabels.none()
    }

    fun isFilterFor(labels: Collection<Label>): Boolean {
        if (!hasFilters()) {
            return true
        }
        return !labels.none { isFilterFor(it) }
    }

    fun isFilterFor(label: Label): Boolean {
        return filterByLabels.containsKey(label.name)
    }

}