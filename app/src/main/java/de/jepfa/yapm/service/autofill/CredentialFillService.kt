package de.jepfa.yapm.service.autofill

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.*
import androidx.annotation.RequiresApi
import de.jepfa.yapm.service.autofill.ResponseFiller.createFillResponse


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

        val contexts = fillRequest.fillContexts
        val structure = contexts.get(contexts.size - 1).structure

        val fillResponse = createFillResponse(structure,
            allowCreateAuthentication = true, context = applicationContext)

        fillCallback.onSuccess(fillResponse)

    }

    override fun onSaveRequest(saveRequest: SaveRequest, saveCallback: SaveCallback) {
    }

}