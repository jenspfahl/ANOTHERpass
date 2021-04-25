package de.jepfa.yapm.service.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.util.TypedValue
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.autofill.ResponseFiller.createFillResponse
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.util.PreferenceUtil


class CredentialFillService: AutofillService() {


    override fun onFillRequest(
        fillRequest: FillRequest,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback
    ) {

        val contexts = fillRequest.fillContexts
        val structure = contexts.get(contexts.size - 1).getStructure()

        val fillResponse = createFillResponse(structure, cancellationSignal,
            createAuthentication = true, context = applicationContext)
        fillResponse?.let {
            fillCallback.onSuccess(it)
        }

    }

    override fun onSaveRequest(saveRequest: SaveRequest, saveCallback: SaveCallback) {
    }



}