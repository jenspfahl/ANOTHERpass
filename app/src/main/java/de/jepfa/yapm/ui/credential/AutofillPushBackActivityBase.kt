package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT
import android.view.inputmethod.InlineSuggestionsRequest
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.service.autofill.AutofillCredentialHolder
import de.jepfa.yapm.service.autofill.ResponseFiller
import de.jepfa.yapm.ui.SecureActivity

abstract class AutofillPushBackActivityBase : SecureActivity() {

    private var assistStructure: AssistStructure? = null
    private var inlineSuggestionsRequest: InlineSuggestionsRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AutofillCredentialHolder.clear()
        if (ResponseFiller.isAutofillSupported()) {
            assistStructure = intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)
        }
        if (ResponseFiller.isInlinePresentationSupported()) {
            inlineSuggestionsRequest = intent.getParcelableExtra(AutofillManager.EXTRA_INLINE_SUGGESTIONS_REQUEST)
        }
        super.onCreate(savedInstanceState)

    }

    fun shouldPushBackAutoFill() : Boolean {
        return assistStructure != null && ResponseFiller.isAutofillSupported()
    }

    fun pushBackAutofill(credential: EncCredential, deobfuscationKey: Key?) {
        AutofillCredentialHolder.update(credential, deobfuscationKey)
        pushBackAutofill()
    }

    fun pushBackAutofill(ignoreCurrentApp: Boolean = false, allowCreateAuthentication: Boolean = false) {
        val structure = assistStructure
        if (structure != null && ResponseFiller.isAutofillSupported()) {
            val replyIntent = Intent().apply {
                ResponseFiller.updateInlinePresentationRequest(inlineSuggestionsRequest)
                val fillResponse = ResponseFiller.createFillResponse(
                    structure,
                    allowCreateAuthentication,
                    ignoreCurrentApp,
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

