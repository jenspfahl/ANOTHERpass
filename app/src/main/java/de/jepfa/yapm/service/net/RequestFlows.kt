package de.jepfa.yapm.service.net

import android.view.View
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.ui.SecureActivity


interface RequestFlows {

    fun getLifeCycleActivity(): SecureActivity
    fun getRootView(): View

    fun resetUi()

    fun startCredentialCreation(name: String, domain: String)
    fun startCredentialUiSearchFor(domain: String)
    fun startCredentialSelectionMode()
    fun getSelectedCredentials(): Set<EncCredential>
    fun stopCredentialSelectionMode()

}