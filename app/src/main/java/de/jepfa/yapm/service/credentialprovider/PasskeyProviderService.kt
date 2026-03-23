package de.jepfa.yapm.service.credentialprovider


import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.provider.ContactsContract.Directory.PACKAGE_NAME
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
import de.jepfa.yapm.BuildConfig.APPLICATION_ID
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.toastText


@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PasskeyProviderService: CredentialProviderService() {
    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        toastText(this, "clear creds")
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>
    ) {
        toastText(this, "get creds")
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        val response: BeginCreateCredentialResponse? = processCreateCredentialRequest(request)
        if (response != null) {
            callback.onResult(response)
        } else {
            callback.onError(CreateCredentialUnknownException())
        }
    }

    fun processCreateCredentialRequest(request: BeginCreateCredentialRequest): BeginCreateCredentialResponse? {
        when (request) {
            is BeginCreatePublicKeyCredentialRequest -> {
                // Request is passkey type
                return handleCreatePasskeyQuery(request)
            }
            is BeginCreatePasswordCredentialRequest -> {
                // Request is password type
                return handleCreatePasswordQuery(request)
            }
        }
        // Request not supported
        return null
    }

    private fun handleCreatePasskeyQuery(
        request: BeginCreatePublicKeyCredentialRequest
    ): BeginCreateCredentialResponse {

        // Adding two create entries - one for storing credentials to the 'Personal'
        // account, and one for storing them to the 'Family' account. These
        // accounts are local to this sample app only.
        val createEntries: MutableList<CreateEntry> = mutableListOf()
        createEntries.add(
            CreateEntry(
                "PassKey TODO",
                createNewPendingIntent("PERSONAL_ACCOUNT_ID", Constants.ACTION_CREATE_PASSKEY)
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
                "Password TODO",
                createNewPendingIntent("FAMILY_ACCOUNT_ID", Constants.ACTION_CREATE_PASSWORD)
            )
        )

        return BeginCreateCredentialResponse(createEntries)
    }

    private fun createNewPendingIntent(accountId: String, action: String): PendingIntent {
        val intent = Intent(this, ListCredentialsActivity::class.java)
            .setPackage(PACKAGE_NAME).setAction(action)

        // Add your local account ID as an extra to the intent, so that when
        // user selects this entry, the credential can be saved to this
        // account
        intent.putExtra(EXTRA_KEY_ACCOUNT_ID, accountId)

        return PendingIntent.getActivity(
            applicationContext, UNIQUE_REQ_CODE,
            intent,
            (
                    PendingIntent.FLAG_MUTABLE
                            or PendingIntent.FLAG_UPDATE_CURRENT
                    )
        )
    }

    companion object {
        const val EXTRA_KEY_ACCOUNT_ID = "keyAccountId"
        const val UNIQUE_REQ_CODE = 666
    }

}
