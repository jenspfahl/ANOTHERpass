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
import de.jepfa.yapm.ui.credential.AutofillPushBackActivityBase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase


class EditCredentialActivity : AutofillPushBackActivityBase() {

    var currentId: Int? = null
    lateinit var current: EncCredential
    var original: EncCredential? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }
        setContentView(R.layout.activity_edit_credential)

        labelViewModel.allLabels.observe(this, { labels ->
            masterSecretKey?.let{ key ->
                LabelService.initLabels(key, labels.toSet())
            }
        })

        val idExtra = intent.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)
        if (idExtra == -1) {
            setTitle(R.string.title_new_credential)
        }
        else {
            currentId = idExtra
            setTitle(R.string.title_change_credential)
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
        val replyIntent = Intent()
        current.applyExtras(replyIntent)

        if (shouldPushBackAutoFill()) {
            AutofillCredentialHolder.update(current, deobfuscationKey)
        }

        setResult(Activity.RESULT_OK, replyIntent)
        finish()
    }

}