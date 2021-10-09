package de.jepfa.yapm.ui.credential

import de.jepfa.yapm.R

enum class CredentialSortOrder(val labelId: Int) {
    CREDENTIAL_NAME_ASC(R.string.credential_sort_by_name_asc),
    CREDENTIAL_NAME_DESC(R.string.credential_sort_by_name_desc),
    RECENTLY_MODIFIED(R.string.credential_sort_by_recently_modified),
    CREDENTIAL_IDENTIFIER(R.string.credential_sort_by_id);

    companion object {
        val DEFAULT = CREDENTIAL_NAME_ASC
    }
}