package de.jepfa.yapm.service.net

import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.Base64
import android.util.Log
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE
import de.jepfa.yapm.service.PreferenceService.PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE
import de.jepfa.yapm.service.autofill.AutofillCredentialHolder
import de.jepfa.yapm.service.io.TempFileService
import de.jepfa.yapm.service.net.HttpServer.serverLog
import de.jepfa.yapm.service.net.HttpServer.toErrorResponse
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.ServerRequestBottomSheet
import de.jepfa.yapm.usecase.vault.ShareVaultUseCase
import de.jepfa.yapm.util.ensureHttp
import de.jepfa.yapm.util.extractDomain
import de.jepfa.yapm.util.observeOnce
import de.jepfa.yapm.util.toReadableString
import de.jepfa.yapm.util.toUUIDOrNull
import de.jepfa.yapm.util.toastText
import io.ktor.http.HttpStatusCode
import io.ktor.http.RequestConnectionPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList
import java.util.UUID


object HttpCredentialRequestHandler {
    private const val SERVER_REQUEST_SNACKBAR_DURATION = 300_000 // this is the maximum timeout of the extension

    private var webClientCredentialRequestState = CredentialRequestState.None
    private var webClientRequestIdentifier: String? = null
    private var webClientRequestedWebsite: String? = null
    private var webClientRequestedUser: String? = null

    var credentialSelectState: MultipleCredentialSelectState = MultipleCredentialSelectState.NONE
    private var serverSnackbar: Snackbar? = null
    private var serverRequestBottomSheets: MutableMap<String, ServerRequestBottomSheet> = HashMap()



    fun isProgressing() = webClientCredentialRequestState.isProgressing

    fun currentRequestState() = webClientCredentialRequestState

    fun getWebsiteSuggestion(): Triple<String, String, String>? {
        if (isProgressing() && webClientRequestedWebsite != null) {
            val suggestedName = extractDomain(webClientRequestedWebsite!!, withTld = false).capitalize()
            val suggestedDomain = extractDomain(webClientRequestedWebsite!!, withTld = true)

            return Triple(suggestedName, suggestedDomain, webClientRequestedUser?:"")
        }
        else {
            return null
        }
    }

    fun reset(requestFlows: RequestFlows) {
        webClientRequestIdentifier = null
        updateRequestState(CredentialRequestState.None, requestFlows)
        webClientRequestedWebsite = null
        webClientRequestedUser = null
    }

    fun handleIncomingRequest(
        key: SecretKeyHolder,
        webExtension: EncWebExtension,
        message: JSONObject,
        requestFlows: RequestFlows,
        origin: RequestConnectionPoint
        ): Pair<HttpStatusCode, JSONObject> {
        Log.d("HTTP", "webClientRequestIdentifier: $webClientRequestIdentifier ($webClientCredentialRequestState)")


        val webClientId = SecretService.decryptCommonString(key, webExtension.webClientId)
        val webClientTitle = SecretService.decryptCommonString(key, webExtension.title)

        val command = try {
            val command = message.getString("command")
            FetchCredentialCommand.getByCommand(command)
        } catch (e: Exception) {
            Log.w("HTTP", "Cannot parse command")
            return toErrorResponse(HttpStatusCode.BadRequest, "unknown command")
        }
        Log.d("HTTP", "command: $command")
        val incomingRequestIdentifier = message.getString("requestIdentifier")
        val website = message.optString("website")
        val user = message.optString("user")
        val uid = message.optString("uid")
        val uids = message.optJSONArray("uids")


        if (webClientRequestIdentifier != incomingRequestIdentifier) {
            if (webClientCredentialRequestState.isProgressing) {
                Log.i("HTTP", "concurrent but ignored credential request $incomingRequestIdentifier for $webClientId: $webClientCredentialRequestState")
                return toErrorResponse(HttpStatusCode.Conflict, "waiting for concurrent request")
            }
            else {
                Log.i("HTTP", "next credential request $incomingRequestIdentifier for $webClientId")
                updateRequestState(CredentialRequestState.Incoming, requestFlows)
                webClientRequestIdentifier = incomingRequestIdentifier
                webClientRequestedWebsite = null
                webClientRequestedUser = null
            }
        }

        serverLog(
            origin = origin,
            webClientId = webClientId,
            msg = "Handling request '${webClientRequestIdentifier?.take(6)?:"??"}' with command ${command.command} and state $webClientCredentialRequestState",
        )

        if (!requestFlows.getLifeCycleActivity().isActivityInForeground()) {
            Log.d("HTTP", "activity ${requestFlows.getLifeCycleActivity()} not in foreground (${requestFlows.getLifeCycleActivity().lifecycle.currentState})")
            return toErrorResponse(HttpStatusCode.Unauthorized, "not in foreground")
        }

        if (command == FetchCredentialCommand.CANCEL_REQUEST) {
            updateRequestState(CredentialRequestState.Denied, requestFlows)
            CoroutineScope(Dispatchers.Main).launch {
                serverRequestBottomSheets[incomingRequestIdentifier]?.dismiss()
                serverRequestBottomSheets.remove(incomingRequestIdentifier)

                serverSnackbar?.dismiss()
                requestFlows.resetUi()
                toastText(
                    requestFlows.getLifeCycleActivity(),
                    requestFlows.getLifeCycleActivity().getString(R.string.request_denied)
                )
            }
        }


        when (webClientCredentialRequestState) {
            CredentialRequestState.Incoming -> {

                val sharedBaseKey = SecretService.decryptKey(key, webExtension.sharedBaseKey)
                val requestIdentifierKey = Key(Base64.decode(webClientRequestIdentifier, 0))
                val fingerprintAsKey = SecretService.conjunctKeys(sharedBaseKey, requestIdentifierKey)
                val shortenedFingerprint = fingerprintAsKey.toShortenedFingerprint()

                CoroutineScope(Dispatchers.Main).launch {

                    AutofillCredentialHolder.clear()
                    requestFlows.resetUi()

                    val uuid = uid.toUUIDOrNull()
                    if (command == FetchCredentialCommand.FETCH_CREDENTIAL_FOR_URL && uuid != null) {
                        startFetchCredentialForUidFlow(
                            requestFlows,
                            uuid,
                            key,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint,
                            website,
                            user
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_CREDENTIAL_FOR_URL  && website.isNotBlank()) {
                        startFetchCredentialForWebsiteFlow(
                            requestFlows,
                            website,
                            user,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_CREDENTIAL_FOR_UID && uuid != null) {
                        startFetchCredentialForUidFlow(
                            requestFlows,
                            uuid,
                            key,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint,
                            null,
                            null
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_CREDENTIALS_FOR_UIDS && uids != null) {
                        startFetchCredentialForUidsFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint,
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_CLIENT_KEY) {
                        startFetchClientKeyFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_SINGLE_CREDENTIAL) {
                        startFetchAnyCredentialFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_MULTIPLE_CREDENTIALS) {
                        startFetchMultipleCredentialsFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_ALL_CREDENTIALS) {
                        startFetchAllCredentialsFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint
                        )
                    }
                    else if (command == FetchCredentialCommand.CREATE_CREDENTIAL_FOR_URL && website.isNotBlank()) {
                        startCreateCredentialForWebsiteFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint,
                            website,
                            user
                        )
                    }
                    else if (command == FetchCredentialCommand.DOWNLOAD_VAULT_BACKUP) {
                        startDownloadVaultBackupFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            incomingRequestIdentifier,
                            shortenedFingerprint
                        )
                    }
                    else {
                        Log.i("HTTP", "unhandled command: $command")
                    }
                }

                return toErrorResponse(HttpStatusCode.Accepted, "no user acknowledge")
            }
            CredentialRequestState.AwaitingAcceptance -> {
                return toErrorResponse(HttpStatusCode.Accepted, "pending request")
            }
            CredentialRequestState.Denied -> {
                webClientRequestIdentifier = null
                updateRequestState(CredentialRequestState.Fulfilled, requestFlows)

                return toErrorResponse(HttpStatusCode.Forbidden, "denied by user")
            }
            CredentialRequestState.Accepted -> {
                if (webClientRequestIdentifier != incomingRequestIdentifier) {
                    return toErrorResponse(HttpStatusCode.BadRequest, "wrong request identifier")
                }
                return if (command == FetchCredentialCommand.FETCH_CLIENT_KEY) {
                    postClientKey(requestFlows, key, webClientId)
                }
                else if (command == FetchCredentialCommand.FETCH_MULTIPLE_CREDENTIALS) {
                    if (credentialSelectState == MultipleCredentialSelectState.USER_COMMITTED) {
                        postSelectedCredentials(requestFlows, key, webClientId)
                    }
                    else {
                        toErrorResponse(HttpStatusCode.Accepted, "no user selection")
                    }
                }
                else if (command == FetchCredentialCommand.FETCH_ALL_CREDENTIALS) {
                    postAllCredentialsExceptVeiled(requestFlows, key, webClientId)
                }
                else if (command == FetchCredentialCommand.FETCH_CREDENTIALS_FOR_UIDS) {
                    postCredentialsByUidsExceptVeiled(requestFlows, key, webClientId, uids)
                }
                else if (command == FetchCredentialCommand.DOWNLOAD_VAULT_BACKUP) {
                    postVaultBackupFileUrl(requestFlows, key, webClientId)
                }
                else {
                    // if a new holder holds a selected cred like AutofillCredentialHolder
                    val currCredential = AutofillCredentialHolder.currentCredential
                    if (currCredential != null) {
                        postCredential(requestFlows, key, webClientId, currCredential)
                    } else {
                        // waiting for user s selection
                        toErrorResponse(HttpStatusCode.Accepted, "no user selection")
                    }
                }
            }
            CredentialRequestState.Fulfilled -> {
                webClientRequestIdentifier = null
                return toErrorResponse(HttpStatusCode.NoContent, "still provided")
            }
            else -> {
                return toErrorResponse(HttpStatusCode.InternalServerError, "unhandled request state: $webClientCredentialRequestState")
            }
        }
    }

    private fun startFetchCredentialForUidFlow(
        requestFlows: RequestFlows,
        uuid: UUID,
        key: SecretKeyHolder,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String,
        website: String?,
        user: String?
    ) {
        findCredentialByUuid(requestFlows.getLifeCycleActivity(), uuid) { credential ->
            if (credential != null) {
                val name = SecretService.decryptCommonString(key, credential.name)
                showClientRequest(
                    requestFlows,
                    webExtension,
                    webClientTitle,
                    webClientId,
                    requestFlows.getLifeCycleActivity().getString(R.string.request_detail_fetch_by_uid, name),
                    incomingRequestIdentifier,
                    shortenedFingerprint,
                    requestFlows.getLifeCycleActivity().getString(R.string.request_user_action_fetch_by_uid, name)
                )
                {
                    AutofillCredentialHolder.update(credential, obfuscationKey = null)
                }
            } else {
                if (website != null) {
                    Log.i("HTTP","Requested credential not found, ask the user to select one")

                    startFetchCredentialForWebsiteFlow(
                        requestFlows,
                        website,
                        user?:"",
                        webExtension,
                        webClientTitle,
                        webClientId,
                        incomingRequestIdentifier,
                        shortenedFingerprint
                    )
                }
                else {
                    toastText(
                        requestFlows.getLifeCycleActivity(),
                        requestFlows.getLifeCycleActivity().getString(R.string.requested_credential_to_sync_not_found)
                    )
                    updateRequestState(CredentialRequestState.Denied, requestFlows)
                }
            }
        }
    }

    private fun startFetchCredentialForUidsFlow(
        requestFlows: RequestFlows,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String,
    ) {

        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            requestFlows.getLifeCycleActivity().getString(R.string.request_detail_fetch_by_uids),
            incomingRequestIdentifier,
            shortenedFingerprint,
            requestFlows.getLifeCycleActivity().getString(R.string.request_user_action_fetch_by_uids)
        )
        {
            // no user interaction
        }

    }

    private fun startFetchCredentialForWebsiteFlow(
        requestFlows: RequestFlows,
        website: String,
        user: String,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String
    ) {
        val domain = extractDomain(website, withTld = true)
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            requestFlows.getLifeCycleActivity().getString(R.string.request_detail_fetch_by_website, domain),
            incomingRequestIdentifier,
            shortenedFingerprint,
            requestFlows.getLifeCycleActivity().getString(R.string.request_user_action_fetch_by_website, domain)
        )
        {
            requestFlows.startCredentialUiSearchFor(domain)
            webClientRequestedWebsite = website
            webClientRequestedUser = user
        }
    }

    private fun startCreateCredentialForWebsiteFlow(
        requestFlows: RequestFlows,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String,
        website: String,
        user: String
    ) {
        val domain = extractDomain(website, withTld = true)
        val name = extractDomain(website, withTld = false).capitalize()
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            requestFlows.getLifeCycleActivity().getString(R.string.request_detail_create_by_website, domain),
            incomingRequestIdentifier,
            shortenedFingerprint,
            requestFlows.getLifeCycleActivity().getString(R.string.request_user_action_create_by_website, domain),
            showSnackbars = false,
        )
        {
            requestFlows.startCredentialCreation(
                name,
                domain,
                user,
                webExtension.id!!,
                shortenedFingerprint,
            )
        }
    }

    private fun startFetchClientKeyFlow(
        requestFlows: RequestFlows,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String
    ) {
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            requestFlows.getLifeCycleActivity().getString(R.string.request_detail_client_key),
            incomingRequestIdentifier,
            shortenedFingerprint,
            requestFlows.getLifeCycleActivity().getString(R.string.request_user_action_client_key)
        )
        {
            // no user interaction
        }
    }

    private fun startFetchAnyCredentialFlow(
        requestFlows: RequestFlows,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String
    ) {
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            requestFlows.getLifeCycleActivity().getString(R.string.request_detail_fetch_any),
            incomingRequestIdentifier,
            shortenedFingerprint,
            requestFlows.getLifeCycleActivity().getString(R.string.request_user_action_fetch_any)
        )
        {
            // no user interaction
        }
    }


    private fun startFetchMultipleCredentialsFlow(
        requestFlows: RequestFlows,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String
    ) {
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,

            requestFlows.getLifeCycleActivity().getString(R.string.request_detail_fetch_multiple),
            incomingRequestIdentifier,
            shortenedFingerprint,
            requestFlows.getLifeCycleActivity().getString(R.string.request_user_action_fetch_multiple)
        )
        {
            // start multiple selection mode
            credentialSelectState = MultipleCredentialSelectState.USER_SELECTING
            requestFlows.startCredentialSelectionMode()

        }
    }

    private fun startFetchAllCredentialsFlow(
        requestFlows: RequestFlows,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String
    ) {
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            requestFlows.getLifeCycleActivity().getString(R.string.request_detail_fetch_all),
            incomingRequestIdentifier,
            shortenedFingerprint,
            requestFlows.getLifeCycleActivity().getString(R.string.request_user_action_fetch_all)
        )
        {
            // no user interaction
        }
    }


    private fun startDownloadVaultBackupFlow(
        requestFlows: RequestFlows,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String,
    ) {
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            requestFlows.getLifeCycleActivity().getString(R.string.request_detail_download_vault_backup),
            incomingRequestIdentifier,
            shortenedFingerprint,
            requestFlows.getLifeCycleActivity().getString(R.string.request_user_action_download_vault_backup),
            showSnackbars = false,
        )
        {
            // no user interaction
        }
    }

    private fun findCredentialByUuid(activity: SecureActivity, uuid: UUID, resolved: (EncCredential?) -> Unit) {
        activity.credentialViewModel.findByUid(uuid)
            .observeOnce(activity, resolved)
    }



    private fun showClientRequest(
        requestFlows: RequestFlows,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        details: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String,
        userActionText: String,
        showSnackbars: Boolean = true,
        acceptHandler: () -> Unit,
    ) {
        Log.d("HTTP", "showClientRequest for $shortenedFingerprint")

        if (webExtension.bypassIncomingRequests) {
            updateRequestState(CredentialRequestState.Accepted, requestFlows)
            acceptHandler()

            if (showSnackbars) {
                showBypassSnackbar(
                    requestFlows,
                    webClientTitle,
                    webClientId,
                    details,
                    shortenedFingerprint,
                    webExtension
                ) {
                    false
                }
            }
        } else {
            showAcceptBottomSheet(
                requestFlows,
                webClientTitle,
                webClientId,
                details,
                incomingRequestIdentifier,
                shortenedFingerprint,
                webExtension,
            )
            {
                acceptHandler()
                if (showSnackbars) {
                    showUserActionSnackbar(
                        requestFlows,
                        userActionText,
                    ) {
                        false
                    }
                }
            }
            updateRequestState(CredentialRequestState.AwaitingAcceptance, requestFlows)
        }
    }

    private fun showAcceptBottomSheet(
        requestFlows: RequestFlows,
        webClientTitle: String,
        webClientId: String,
        details: String,
        incomingRequestIdentifier: String,
        shortenedFingerprint: String,
        webExtension: EncWebExtension,
        acceptHandler: () -> Unit,
    ) {
        serverRequestBottomSheets[incomingRequestIdentifier]?.dismiss()
        serverRequestBottomSheets.remove(incomingRequestIdentifier)

        val serverRequestBottomSheet = ServerRequestBottomSheet(
            requestFlows.getLifeCycleActivity(),
            webClientTitle = webClientTitle,
            webClientId = webClientId,
            webRequestDetails = details,
            fingerprint = shortenedFingerprint,
            denyHandler = { allowBypass ->
                webExtension.bypassIncomingRequests = allowBypass
                requestFlows.getLifeCycleActivity().webExtensionViewModel.save(
                    webExtension,
                    requestFlows.getLifeCycleActivity()
                )

                updateRequestState(CredentialRequestState.Denied, requestFlows)
                toastText(
                    requestFlows.getLifeCycleActivity(),
                    requestFlows.getLifeCycleActivity().getString(R.string.request_denied)
                )
                requestFlows.resetUi()
            },
            acceptHandler = { allowBypass ->
                webExtension.bypassIncomingRequests = allowBypass
                requestFlows.getLifeCycleActivity().webExtensionViewModel.save(
                    webExtension,
                    requestFlows.getLifeCycleActivity()
                )

                updateRequestState(CredentialRequestState.Accepted, requestFlows)

                acceptHandler()
            }
        )
        serverRequestBottomSheet.show()
        serverRequestBottomSheets[incomingRequestIdentifier] = serverRequestBottomSheet
    }

    fun showUserActionSnackbar(
        requestFlows: RequestFlows,
        text: String,
        denyRequestVeto: () -> Boolean
    ) {
        serverSnackbar?.dismiss()
        serverSnackbar = Snackbar.make(
            requestFlows.getRootView(),
            text,
            SERVER_REQUEST_SNACKBAR_DURATION
        )
            .setAction(requestFlows.getLifeCycleActivity().getString(R.string.cancel_request)) {
                updateRequestState(CredentialRequestState.Denied, requestFlows)

                requestFlows.resetUi()
                toastText(requestFlows.getLifeCycleActivity(), requestFlows.getLifeCycleActivity().getString(R.string.request_denied))

            }
            .setTextMaxLines(7)
            .addCallback(object : BaseCallback<Snackbar>() {
                override fun onDismissed(bar: Snackbar, event: Int) {
                    credentialSelectState = MultipleCredentialSelectState.NONE //do this before reset UI

                    requestFlows.resetUi()
                    requestFlows.getRootView().updatePadding(bottom = 0)

                    if (!denyRequestVeto() && webClientCredentialRequestState.isProgressing) {
                        updateRequestState(CredentialRequestState.Denied, requestFlows)
                        toastText(
                            requestFlows.getLifeCycleActivity(),
                            requestFlows.getLifeCycleActivity().getString(R.string.request_denied)
                        )
                    }
                }

                override fun onShown(bar: Snackbar) {
                    requestFlows.getRootView().updatePadding(bottom = bar.view.measuredHeight)
                }
            })

        serverSnackbar?.show()
    }

    fun showBypassSnackbar(
        requestFlows: RequestFlows,
        webClientTitle: String,
        webClientId: String,
        details: String,
        shortenedFingerprint: String,
        webExtension: EncWebExtension,
        denyRequestVeto: () -> Boolean
    ) {

        val swipeToCancel = requestFlows.getLifeCycleActivity().getString(R.string.swipe_to_cancel)
        val fingerprint = requestFlows.getLifeCycleActivity().getString(R.string.fingerprint)
        val span =
            SpannableString("$webClientTitle ($webClientId) $details $swipeToCancel. ${fingerprint.capitalize()}: $shortenedFingerprint")

        span.setSpan(
            ForegroundColorSpan(requestFlows.getLifeCycleActivity().getColor(R.color.colorAltAccent)),
            0, webClientTitle.length + webClientId.length + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        span.setSpan(
            StyleSpan(Typeface.BOLD),
            0, webClientTitle.length + webClientId.length + 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            span.setSpan(
                TypefaceSpan(Typeface.MONOSPACE),
                span.length - shortenedFingerprint.length,
                span.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        span.setSpan(
            StyleSpan(Typeface.BOLD),
            span.length - shortenedFingerprint.length,
            span.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        serverSnackbar?.dismiss()
        serverSnackbar = Snackbar.make(
            requestFlows.getRootView(),
            span,
            SERVER_REQUEST_SNACKBAR_DURATION
        )
            .setAction(requestFlows.getLifeCycleActivity().getString(R.string.deny_and_revoke_bypass)) {
                updateRequestState(CredentialRequestState.Denied, requestFlows)
                webExtension.bypassIncomingRequests = false
                requestFlows.getLifeCycleActivity().webExtensionViewModel.save(webExtension, requestFlows.getLifeCycleActivity())
                requestFlows.resetUi()
                toastText(requestFlows.getLifeCycleActivity(), requestFlows.getLifeCycleActivity().getString(R.string.request_denied_and_bypass_revoked))

            }
            .setTextMaxLines(7)
            .addCallback(object : BaseCallback<Snackbar>() {
                override fun onDismissed(bar: Snackbar, event: Int) {
                    requestFlows.resetUi()
                    requestFlows.getRootView().updatePadding(bottom = 0)


                    if (!denyRequestVeto() && webClientCredentialRequestState.isProgressing) {
                        updateRequestState(CredentialRequestState.Denied, requestFlows)
                        toastText(requestFlows.getLifeCycleActivity(), requestFlows.getLifeCycleActivity().getString(R.string.request_denied))
                    }
                }

                override fun onShown(bar: Snackbar) {
                    requestFlows.getRootView().updatePadding(bottom = bar.view.measuredHeight)
                }
            })

        serverSnackbar?.show()
    }

    private fun postCredential(
        requestFlows: RequestFlows,
        key: SecretKeyHolder,
        webClientId: String,
        currCredential: EncCredential
    ): Pair<HttpStatusCode, JSONObject> {
        val (name, responseCredential) = mapCredential(key, currCredential, deobfuscate = true)

        val clientKey = SecretService.deriveClientKey(key, webClientId, requestFlows.getLifeCycleActivity())

        val response = JSONObject()

        response.put("credential", responseCredential)
        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        updateRequestState(CredentialRequestState.Fulfilled, requestFlows)

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            toastText(requestFlows.getLifeCycleActivity(),
                requestFlows.getLifeCycleActivity().getString(R.string.credential_posted, name))
        }

        return Pair(HttpStatusCode.OK, response)
    }

    private fun postSelectedCredentials(
        requestFlows: RequestFlows,
        key: SecretKeyHolder,
        webClientId: String,
    ): Pair<HttpStatusCode, JSONObject> {


        val response = JSONObject()
        val responseCredentials = JSONArray()

        requestFlows.getSelectedCredentials().forEach { it ->
            val (_, responseCredential) = mapCredential(key, it, deobfuscate = true)
            responseCredentials.put(responseCredential)
        }

        credentialSelectState = MultipleCredentialSelectState.NONE


        val clientKey = SecretService.deriveClientKey(key, webClientId, requestFlows.getLifeCycleActivity())

        response.put("credentials", responseCredentials)
        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        updateRequestState(CredentialRequestState.Fulfilled, requestFlows)

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            requestFlows.resetUi()

            toastText(requestFlows.getLifeCycleActivity(),
                requestFlows.getLifeCycleActivity().getString(R.string.selected_credentials_posted))
        }

        return Pair(HttpStatusCode.OK, response)
    }

    private fun postAllCredentialsExceptVeiled(
        requestFlows: RequestFlows,
        key: SecretKeyHolder,
        webClientId: String,
    ): Pair<HttpStatusCode, JSONObject> {

        val response = JSONObject()
        val responseCredentials = JSONArray()

        val allCredentials = requestFlows.getLifeCycleActivity().getApp().credentialRepository.getAllSync()
        allCredentials
            .filter { !it.passwordData.isObfuscated }
            .forEach {
                val (_, responseCredential) = mapCredential(key, it, deobfuscate = true)
                responseCredentials.put(responseCredential)
            }

        val clientKey = SecretService.deriveClientKey(key, webClientId, requestFlows.getLifeCycleActivity())


        response.put("credentials", responseCredentials)
        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        updateRequestState(CredentialRequestState.Fulfilled, requestFlows)
        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            toastText(requestFlows.getLifeCycleActivity(), requestFlows.getLifeCycleActivity().getString(R.string.all_credentials_posted))
        }

        return Pair(HttpStatusCode.OK, response)
    }

    private fun postCredentialsByUidsExceptVeiled(
        requestFlows: RequestFlows,
        key: SecretKeyHolder,
        webClientId: String,
        uidsAsJSONArray: JSONArray?
    ): Pair<HttpStatusCode, JSONObject> {

        val uids = LinkedList<UUID>()
        if (uidsAsJSONArray != null) {
            for (i in 0 until uidsAsJSONArray.length()) {
                val uid = uidsAsJSONArray.optString(i).toUUIDOrNull()
                if (uid != null) {
                    uids.add(uid)
                }
            }
        }

        val response = JSONObject()
        val responseCredentials = JSONArray()

        val credentials = requestFlows.getLifeCycleActivity().getApp().credentialRepository.getAllByUidsSync(uids)
        credentials
            .filter { !it.passwordData.isObfuscated }
            .forEach {
                val (_, responseCredential) = mapCredential(key, it, deobfuscate = true)
                responseCredentials.put(responseCredential)
            }

        val clientKey = SecretService.deriveClientKey(key, webClientId, requestFlows.getLifeCycleActivity())


        response.put("credentials", responseCredentials)
        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        updateRequestState(CredentialRequestState.Fulfilled, requestFlows)

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            toastText(requestFlows.getLifeCycleActivity(), requestFlows.getLifeCycleActivity().getString(R.string.credentials_posted))
        }

        return Pair(HttpStatusCode.OK, response)
    }

    private fun postVaultBackupFileUrl(
        requestFlows: RequestFlows,
        key: SecretKeyHolder,
        webClientId: String,
    ): Pair<HttpStatusCode, JSONObject> {
        val includeSettings = PreferenceService.getAsBool(
            PREF_INCLUDE_SETTINGS_IN_BACKUP_FILE, true, requestFlows.getLifeCycleActivity())
        val includeMasterKey = PreferenceService.getAsBool(
            PREF_INCLUDE_MASTER_KEY_IN_BACKUP_FILE, true, requestFlows.getLifeCycleActivity())
        val input = ShareVaultUseCase.Input(includeMasterKey, includeSettings)

        val vaultFile = ShareVaultUseCase.createVaultTempFile(input, requestFlows.getLifeCycleActivity())
        if (vaultFile == null) {
            return toErrorResponse(HttpStatusCode.InternalServerError, "Cannot create vault backup")
        }
        TempFileService.holdVaultBackupFile(vaultFile)

        val clientKey = SecretService.deriveClientKey(key, webClientId, requestFlows.getLifeCycleActivity())

        val downloadKey = HttpServer.generateAndRegisterDownloadKey(webClientId, requestFlows.getLifeCycleActivity())
        val response = JSONObject()

        response.put("clientKey", clientKey.toBase64String())
        response.put("downloadKey", downloadKey)
        response.put("filename", vaultFile.name)

        clientKey.clear()

        webClientRequestIdentifier = null
        updateRequestState(CredentialRequestState.Fulfilled, requestFlows)

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            toastText(requestFlows.getLifeCycleActivity(), requestFlows.getLifeCycleActivity().getString(R.string.vault_backup_file_sent))
        }

        return Pair(HttpStatusCode.OK, response)
    }

    private fun postClientKey(
        requestFlows: RequestFlows,
        key: SecretKeyHolder,
        webClientId: String,
    ): Pair<HttpStatusCode, JSONObject> {

        val clientKey = SecretService.deriveClientKey(key, webClientId, requestFlows.getLifeCycleActivity())
        val response = JSONObject()

        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        updateRequestState(CredentialRequestState.Fulfilled, requestFlows)

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            toastText(requestFlows.getLifeCycleActivity(), requestFlows.getLifeCycleActivity().getString(R.string.local_vault_unlocked))
        }

        return Pair(HttpStatusCode.OK, response)
    }

    private fun mapCredential(
        key: SecretKeyHolder,
        credential: EncCredential,
        deobfuscate: Boolean
    ): Pair<String, JSONObject> {
        val password = SecretService.decryptPassword(key, credential.passwordData.password)
        val user = SecretService.decryptCommonString(key, credential.user)
        val name = SecretService.decryptCommonString(key, credential.name)
        val website = SecretService.decryptCommonString(key, credential.website)
        val uid = credential.uid

        if (deobfuscate) {
            AutofillCredentialHolder.obfuscationKey?.let {
                password.deobfuscate(it)
            }
        }

        val responseCredential = JSONObject()
        if (uid != null) {
            responseCredential.put("uid", uid)
            responseCredential.put("readableUid", uid.toReadableString())
        }
        responseCredential.put("name", name)
        responseCredential.put("password", password.toRawFormattedPassword())
        responseCredential.put("user", user)
        responseCredential.put("website", ensureHttp(website))
        password.clear()
        return Pair(name, responseCredential)
    }


    private fun updateRequestState(newState: CredentialRequestState, requestFlows: RequestFlows) {
        val oldState = webClientCredentialRequestState
        webClientCredentialRequestState = newState
        requestFlows.notifyRequestStateUpdated(oldState, newState)
    }

}