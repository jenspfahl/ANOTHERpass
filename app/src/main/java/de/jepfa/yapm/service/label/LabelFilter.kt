package de.jepfa.yapm.service.label

import android.content.Context
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_USED_LABEL_FILTER
import de.jepfa.yapm.ui.label.Label
import java.util.concurrent.ConcurrentHashMap

object LabelFilter {
    const val WITH_NO_LABELS_ID = -1

    private val filterByLabelIds: MutableMap<Int, Label> = ConcurrentHashMap(32)
    private var filterByNoLabels = false

    fun initState(context: Context, allLabels: List<Label>) {
        val idsAsString = PreferenceService.getAsStringSet(DATA_USED_LABEL_FILTER, context)
        val ids = idsAsString?.map { it.toInt() }
        ids?.forEach { id ->
            if (id == WITH_NO_LABELS_ID) {
                filterByNoLabels = true
            }
            val label = allLabels.find { it.labelId == id }
            if (label != null) {
                filterByLabelIds.putIfAbsent(id, label)
            }
        }
    }

    fun persistState(context: Context) {
        val idsAsString = filterByLabelIds.keys.map { it.toString() }.toSet()
        PreferenceService.putStringSet(DATA_USED_LABEL_FILTER, idsAsString, context)
    }

    fun setFilterFor(label: Label) {
        if (isAssociatedWithNoLabels(label)) {
            filterByNoLabels = true
        }
        label.labelId?.let { filterByLabelIds.put(it, label) }
    }

    fun unsetFilterFor(label: Label) {
        if (isAssociatedWithNoLabels(label)) {
            filterByNoLabels = false
        }
        label.labelId?.let { filterByLabelIds.remove(it, label) }
    }

    fun unsetAllFilters() {
        filterByNoLabels = false
        filterByLabelIds.clear()
    }

    fun hasFilters(): Boolean {
        return !filterByLabelIds.none()
    }

    fun isFilterFor(labels: Collection<Label>): Boolean {
        if (!hasFilters()) {
            return true
        }
        return (labels.isEmpty() && filterByNoLabels) || !labels.none { isFilterFor(it) }
    }

    fun isFilterFor(label: Label): Boolean {
        return filterByLabelIds.containsKey(label.labelId) || (isAssociatedWithNoLabels(label) && filterByNoLabels)
    }

    private fun isAssociatedWithNoLabels(label: Label) =
        label.labelId == WITH_NO_LABELS_ID

}