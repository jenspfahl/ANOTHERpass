package de.jepfa.yapm.service.autofill

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import androidx.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.O)
class CredentialFillService: AutofillService() {


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onFillRequest(
        fillRequest: FillRequest,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback
    ) {
        if (cancellationSignal.isCanceled) {
            fillCallback.onSuccess(null)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ResponseFiller.updateInlinePresentationRequest(fillRequest.inlineSuggestionsRequest)
        }

        val contexts = fillRequest.fillContexts
        val structure = contexts.get(contexts.size - 1).structure
        val fillResponse = ResponseFiller.createFillResponse(
            structure,
            allowCreateAuthentication = true, context = applicationContext)

        fillCallback.onSuccess(fillResponse)

    }

    override fun onSaveRequest(saveRequest: SaveRequest, saveCallback: SaveCallback) {
    }

}