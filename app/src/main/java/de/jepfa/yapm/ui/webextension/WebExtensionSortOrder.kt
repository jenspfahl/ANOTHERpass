package de.jepfa.yapm.ui.webextension

import de.jepfa.yapm.R

enum class WebExtensionSortOrder(val labelId: Int) {
    TITLE(R.string.webextension_sort_by_name),
    WEB_CLIENT_ID(R.string.webextension_sort_by_id),
    RECENTLY_USED(R.string.webextension_sort_by_last_used);

    companion object {
        val DEFAULT = WEB_CLIENT_ID
    }
}