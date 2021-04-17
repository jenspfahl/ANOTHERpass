package de.jepfa.yapm.service.label

import java.util.concurrent.ConcurrentHashMap

object LabelFilter {
    private val filterByLabels: MutableMap<String, LabelService.Label> = ConcurrentHashMap(32)


    fun setFilterFor(label: LabelService.Label): LabelService.Label? {
        return filterByLabels.put(label.labelChip.label, label)
    }

    fun unsetFilterFor(label: LabelService.Label): LabelService.Label? {
        return filterByLabels.remove(label.labelChip.label)
    }

    fun unsetAllFilters() {
        filterByLabels.clear()
    }

    fun hasFilters(): Boolean {
        return !filterByLabels.none()
    }

    fun isFilterFor(labels: Collection<LabelService.Label>): Boolean {
        if (!hasFilters()) {
            return true
        }
        return !labels.none { isFilterFor(it) }
    }

    fun isFilterFor(label: LabelService.Label): Boolean {
        return filterByLabels.containsKey(label.labelChip.label)
    }

}