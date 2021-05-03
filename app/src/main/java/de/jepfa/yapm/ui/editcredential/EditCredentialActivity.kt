package de.jepfa.yapm.ui.editcredential

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.LockVaultUseCase


class EditCredentialActivity : SecureActivity() {

    var currentId: Int? = null
    lateinit var current: EncCredential

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }
        setContentView(R.layout.activity_edit_credential)

        labelViewModel.allLabels.observe(this, { labels ->
            val key = masterSecretKey
            if (key != null) {
                LabelService.initLabels(key, labels.toSet())
            }
        })

        val idExtra = intent.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)
        if (idExtra == -1) {
            setTitle(R.string.title_new_credential)
        }
        else {
            currentId = idExtra
            setTitle(R.string.title_update_credential)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun lock() {
        //generatedPassword.clear()
        recreate()
    }

    fun isUpdate(): Boolean {
        return currentId != null
    }

    fun load(): LiveData<EncCredential> {
        return credentialViewModel.getById(currentId!!)
    }

    fun reply() {
        val replyIntent = Intent()
        current.applyExtras(replyIntent)

        setResult(Activity.RESULT_OK, replyIntent)
        finish()
    }

}