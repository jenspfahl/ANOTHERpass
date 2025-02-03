package de.jepfa.yapm.service.net

import android.view.View
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.ui.SecureActivity


interface RequestFlows {

    fun getLifeCycleActivity(): SecureActivity
    fun getRootView(): View

    fun startCredentialCreation(
        name: String,
        domain: String,
        user: String,
        webExtensionId: Int,
        shortenedFingerprint: String,
    )
    fun startCredentialUiSearchFor(domain: String)
    fun startCredentialSelectionMode()
    fun getSelectedCredentials(): Set<EncCredential>

    fun resetUi()

    fun notifyRequestStateUpdated(oldState: CredentialRequestState, newState: CredentialRequestState) {}
}