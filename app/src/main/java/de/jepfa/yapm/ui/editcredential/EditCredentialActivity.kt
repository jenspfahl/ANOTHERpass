package de.jepfa.yapm.ui.editcredential

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.autofill.AutofillCredentialHolder
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.net.HttpCredentialRequestHandler
import de.jepfa.yapm.service.net.RequestFlows
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.credential.AutofillPushBackActivityBase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.Constants.ACTION_DELIMITER
import de.jepfa.yapm.util.enrichId
import de.jepfa.yapm.util.observeOnce


class EditCredentialActivity : AutofillPushBackActivityBase(), RequestFlows {

    private lateinit var rootView: View
    internal var suggestedCredentialName: String? = null
    internal var suggestedWebSite: String? = null
    internal var suggestedUser: String? = null
    internal var currentId: Int? = null
    internal var current: EncCredential? = null
    internal var original: EncCredential? = null
    internal var saved = false

    public override fun onCreate(savedInstanceState: Bundle?) {

        val idExtra = intent.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)
        if (idExtra == -1) {
            setTitle(R.string.title_new_credential)
        }
        else {
            currentId = idExtra
        }
        savedInstanceState?.getParcelable<Intent>("current")?.let {
            current = EncCredential.fromIntent(it)
        }
        savedInstanceState?.getParcelable<Intent>("original")?.let {
            original = EncCredential.fromIntent(it)
            original?.let {updateTitle(it) }
        }

        intent?.action?.let { action ->
            if (action.startsWith(Constants.ACTION_OPEN_VAULT_FOR_AUTOFILL)) {
                val splitted = action.split(ACTION_DELIMITER)
                suggestedCredentialName = splitted.getOrNull(1)
                suggestedWebSite = splitted.getOrNull(2)
                suggestedUser = splitted.getOrNull(3)
            }
            else if (isOpenedFromWebExtension()) {
                suggestedCredentialName = intent.getStringExtra("name")
                suggestedWebSite = intent.getStringExtra("domain")
                suggestedUser = intent.getStringExtra("user")
            }

        }

        super.onCreate(savedInstanceState)
        
        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }
        setContentView(R.layout.activity_edit_credential)

        rootView = findViewById(R.id.edit_credential)

        labelViewModel.allLabels.observe(this) { labels ->
            masterSecretKey?.let { key ->
                LabelService.defaultHolder.initLabels(key, labels.toSet())
            }
        }


        if (isOpenedFromWebExtension()) {
            val webExtensionId = intent.getIntExtra("webExtensionId", 0)

            webExtensionViewModel.getById(webExtensionId).observeOnce(this) { webExtension ->
                val shortenedFingerprint = intent.getStringExtra("shortenedFingerprint")
                masterSecretKey?.let { key ->
                    val webClientId = SecretService.decryptCommonString(key, webExtension.webClientId)
                    val webClientTitle = SecretService.decryptCommonString(key, webExtension.title)
                    if (webExtension.bypassIncomingRequests) {
                        HttpCredentialRequestHandler.showBypassSnackbar(
                            this,
                            webClientTitle?:"??",
                            webClientId?:"??",
                            "wants to create a new credential for '${suggestedWebSite ?: "??"}'.",
                            shortenedFingerprint?:"??",
                            webExtension,
                        ) {
                            saved
                        }
                    } else {
                        HttpCredentialRequestHandler.showUserActionSnackbar(
                            this,
                            "Create a new credential for '${suggestedWebSite ?: "??"}' to fulfill the request.",
                        )
                        {
                            saved
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        current?.let { current ->
            val asIntent = Intent()
            current.applyExtras(asIntent)
            outState.putParcelable("current", asIntent)
        }
        original?.let { original ->
            val asIntent = Intent()
            original.applyExtras(asIntent)
            outState.putParcelable("original", asIntent)
        }
    }

    override fun lock() {
        recreate()
    }

    fun isUpdate(): Boolean {
        return currentId != null
    }

    fun load(): LiveData<EncCredential> {
        return credentialViewModel.getById(currentId!!)
    }

    fun reply(deobfuscationKey: Key?) {
        current?.let { current ->
            val replyIntent = Intent()
            current.applyExtras(replyIntent)

            if (shouldPushBackAutoFill() || isOpenedFromWebExtension()) {
                AutofillCredentialHolder.update(current, deobfuscationKey)
            }

            setResult(Activity.RESULT_OK, replyIntent)
        }
        finish()
    }

    internal fun updateTitle(credential: EncCredential) {
        masterSecretKey?.let { key ->
            val origName = SecretService.decryptCommonString(key, credential.name)
            val enrichedName = enrichId(this, origName, credential.id)
            title = getString(R.string.title_change_credential_with_title, enrichedName)
        }
    }

    private fun isOpenedFromWebExtension() = intent?.action == Constants.ACTION_PREFILLED_FROM_EXTENSION


    override fun getLifeCycleActivity(): SecureActivity {
        return this
    }

    override fun getRootView(): View {
        return rootView
    }

    override fun startCredentialCreation(
        name: String,
        domain: String,
        user: String,
        webExtensionId: Int,
        shortenedFingerprint: String,
    ) {
    }

    override fun startCredentialUiSearchFor(domain: String) {
    }

    override fun startCredentialSelectionMode() {
    }

    override fun getSelectedCredentials(): Set<EncCredential> {
        return emptySet()
    }

    override fun stopCredentialSelectionMode() {
    }

    override fun resetUi() {
    }
}