package de.jepfa.yapm.ui.credential

import de.jepfa.yapm.R

enum class CredentialGrouping(val labelId: Int) {
    NO_GROUPING(R.string.credential_group_by_none),
    BY_CREDENTIAL_NAME(R.string.credential_group_by_name),
    BY_LABEL(R.string.credential_group_by_label);

    companion object {
        val DEFAULT = NO_GROUPING
    }
}