package de.jepfa.yapm.service.credentialprovider


import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.provider.ContactsContract.Directory.PACKAGE_NAME
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePasswordCredentialRequest
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.webauthn.PublicKeyCredentialCreationOptions
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.getAppNameFromPackage
import de.jepfa.yapm.util.toastText


@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasskeyProviderService: CredentialProviderService() {
    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        //TODO
        toastText(this, "clear creds")
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        //TODO
        toastText(this, "get creds")
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        val response = processCreateCredentialRequest(request)
        if (response != null) {
            callback.onResult(response)
        } else {
            callback.onError(CreateCredentialUnknownException())
        }
    }

    private fun processCreateCredentialRequest(request: BeginCreateCredentialRequest): BeginCreateCredentialResponse? {
        when (request) {
            is BeginCreatePublicKeyCredentialRequest -> {
                // Request is passkey type
                return handleCreatePasskeyQuery(request)
            }
            is BeginCreatePasswordCredentialRequest -> {
                // Request is password type
                return handleCreatePasswordQuery(request)
            }
            else -> Log.w("PK", "Unknown request: $request")
        }
        // Request not supported
        return null
    }

    private fun handleCreatePasskeyQuery(
        request: BeginCreatePublicKeyCredentialRequest
    ): BeginCreateCredentialResponse {

        val creationOptions = PublicKeyCredentialCreationOptions(request.requestJson)

        val createEntries: MutableList<CreateEntry> = mutableListOf()
        createEntries.add(
            CreateEntry(
                 creationOptions.rp.name, // or getAppNameFromPackage(request.callingAppInfo?.packageName, this)
                createNewPendingIntent(Constants.ACTION_CREATE_PASSKEY, REQ_CODE_FOR_CREATE_PASSKEY)
            )
        )

        return BeginCreateCredentialResponse(createEntries)
    }

    private fun handleCreatePasswordQuery(
        request: BeginCreatePasswordCredentialRequest
    ): BeginCreateCredentialResponse {
        val createEntries: MutableList<CreateEntry> = mutableListOf()

        // Adding two create entries - one for storing credentials to the 'Personal'
        // account, and one for storing them to the 'Family' account. These
        // accounts are local to this sample app only.
        createEntries.add(
            CreateEntry(
                getAppNameFromPackage(request.callingAppInfo?.packageName, this) ?: "",
                createNewPendingIntent(Constants.ACTION_CREATE_PASSWORD, REQ_CODE_FOR_CREATE_PASSWORD)
            )
        )

        return BeginCreateCredentialResponse(createEntries)
    }

    private fun createNewPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ListCredentialsActivity::class.java)
            .setPackage(PACKAGE_NAME)
            .setAction(action)

        return PendingIntent.getActivity(
            applicationContext, requestCode,
            intent,
            (
                    PendingIntent.FLAG_MUTABLE
                            or PendingIntent.FLAG_UPDATE_CURRENT
                    )
        )
    }

    companion object {
        const val REQ_CODE_FOR_CREATE_PASSKEY = 100_000_000
        const val REQ_CODE_FOR_CREATE_PASSWORD = 500_000_000
    }

}
