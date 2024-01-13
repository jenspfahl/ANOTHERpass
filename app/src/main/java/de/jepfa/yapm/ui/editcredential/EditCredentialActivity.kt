package de.jepfa.yapm.ui.editcredential

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.autofill.AutofillCredentialHolder
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.credential.AutofillPushBackActivityBase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.Constants.ACTION_DELIMITER
import de.jepfa.yapm.util.enrichId


class EditCredentialActivity : AutofillPushBackActivityBase() {

    internal var suggestedCredentialName: String? =null
    internal var suggestedWebSite: String? =null
    internal var currentId: Int? = null
    internal var current: EncCredential? = null
    internal var original: EncCredential? = null

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
                suggestedCredentialName = action.substringAfter(ACTION_DELIMITER).substringBeforeLast(ACTION_DELIMITER)
                suggestedWebSite = action.substringAfterLast(ACTION_DELIMITER)
            }
        }

        super.onCreate(savedInstanceState)
        
        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }
        setContentView(R.layout.activity_edit_credential)

        labelViewModel.allLabels.observe(this) { labels ->
            masterSecretKey?.let { key ->
                LabelService.defaultHolder.initLabels(key, labels.toSet())
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

            if (shouldPushBackAutoFill()) {
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

}