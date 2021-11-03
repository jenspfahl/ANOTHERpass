package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.service.autofill.AutofillCredentialHolder
import de.jepfa.yapm.service.autofill.ResponseFiller
import de.jepfa.yapm.ui.SecureActivity

abstract class AutofillPushBackActivityBase : SecureActivity() {

    private var assistStructure: AssistStructure? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AutofillCredentialHolder.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assistStructure = intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)
        }

    }

    fun shouldPushBackAutoFill() : Boolean {
        return assistStructure != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    fun pushBackAutofill(credential: EncCredential, deobfuscationKey: Key?) {
        AutofillCredentialHolder.update(credential, deobfuscationKey)
        pushBackAutofill()
    }

    fun pushBackAutofill() {
        val structure = assistStructure
        if (structure != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val replyIntent = Intent().apply {
                val fillResponse = ResponseFiller.createFillResponse(
                    structure,
                    allowCreateAuthentication = false,
                    applicationContext
                )
                putExtra(EXTRA_AUTHENTICATION_RESULT, fillResponse)
            }
            assistStructure = null
            setResult(Activity.RESULT_OK, replyIntent)
            finish()

        }
    }

}

