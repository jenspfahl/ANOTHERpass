package de.jepfa.yapm.service.autofill

import android.os.CancellationSignal
import android.service.autofill.*
import de.jepfa.yapm.service.autofill.ResponseFiller.createFillResponse


class CredentialFillService: AutofillService() {


    override fun onFillRequest(
        fillRequest: FillRequest,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback
    ) {

        val contexts = fillRequest.fillContexts
        val structure = contexts.get(contexts.size - 1).structure

        val fillResponse = createFillResponse(structure, cancellationSignal,
            createAuthentication = true, context = applicationContext)
        fillResponse?.let {
            fillCallback.onSuccess(it)
        }

    }

    override fun onSaveRequest(saveRequest: SaveRequest, saveCallback: SaveCallback) {
    }

}