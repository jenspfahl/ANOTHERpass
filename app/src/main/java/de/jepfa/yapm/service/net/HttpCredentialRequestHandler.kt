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
import android.view.ViewGroup
import androidx.core.view.updatePadding
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.autofill.AutofillCredentialHolder
import de.jepfa.yapm.service.net.HttpServer.toErrorResponse
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.ServerRequestBottomSheet
import de.jepfa.yapm.util.ensureHttp
import de.jepfa.yapm.util.extractDomain
import de.jepfa.yapm.util.observeOnce
import de.jepfa.yapm.util.toReadableString
import de.jepfa.yapm.util.toUUIDOrNull
import de.jepfa.yapm.util.toastText
import io.ktor.http.HttpStatusCode
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


    fun isProgressing() = webClientCredentialRequestState.isProgressing

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

    fun reset() {
        webClientRequestIdentifier = null
        webClientCredentialRequestState = CredentialRequestState.None
        webClientRequestedWebsite = null
        webClientRequestedUser = null
    }

    fun handleIncomingRequest(
        key: SecretKeyHolder,
        webExtension: EncWebExtension,
        message: JSONObject,
        requestFlows: RequestFlows,
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
                webClientCredentialRequestState = CredentialRequestState.Incoming
                webClientRequestIdentifier = incomingRequestIdentifier
                webClientRequestedWebsite = null
                webClientRequestedUser = null
            }
        }

        when (webClientCredentialRequestState) {
            CredentialRequestState.Incoming -> {
                webClientCredentialRequestState = CredentialRequestState.AwaitingAcceptance

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
                            shortenedFingerprint
                        )
                    }
                    if (command == FetchCredentialCommand.FETCH_CREDENTIAL_FOR_UID && uuid != null) {
                        startFetchCredentialForUidFlow(
                            requestFlows,
                            uuid,
                            key,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            shortenedFingerprint,
                            null,
                            null
                        )
                    }
                    if (command == FetchCredentialCommand.FETCH_CREDENTIALS_FOR_UIDS && uids != null) {
                        startFetchCredentialForUidsFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            shortenedFingerprint,
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_CLIENT_KEY) {
                        startFetchClientKeyFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            shortenedFingerprint
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_SINGLE_CREDENTIAL) {
                        startFetchAnyCredentialFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            shortenedFingerprint
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_MULTIPLE_CREDENTIALS) {
                        startFetchMultipleCredentialsFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            shortenedFingerprint
                        )
                    }
                    else if (command == FetchCredentialCommand.FETCH_ALL_CREDENTIALS) {
                        startFetchAllCredentialsFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            shortenedFingerprint
                        )
                    }
                    else if (command == FetchCredentialCommand.CREATE_CREDENTIAL_FOR_URL && website.isNotBlank()) {
                        startCreateCredentialForWebsiteFlow(
                            requestFlows,
                            webExtension,
                            webClientTitle,
                            webClientId,
                            shortenedFingerprint,
                            website,
                            user
                        )
                    }
                    else {
                        Log.i("HTTP", "unhandled command: $command")
                    }
                }

                return toErrorResponse(HttpStatusCode.NotFound, "no user acknowledge")
            }
            CredentialRequestState.AwaitingAcceptance -> {
                return toErrorResponse(HttpStatusCode.NotFound, "pending request")
            }
            CredentialRequestState.Denied -> {
                webClientRequestIdentifier = null
                webClientCredentialRequestState = CredentialRequestState.Fulfilled

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
                        toErrorResponse(HttpStatusCode.NotFound, "no user selection")
                    }
                }
                else if (command == FetchCredentialCommand.FETCH_ALL_CREDENTIALS) {
                    postAllCredentialsExceptVeiled(requestFlows, key, webClientId)
                }
                else if (command == FetchCredentialCommand.FETCH_CREDENTIALS_FOR_UIDS) {
                    postCredentialsByUidsExceptVeiled(requestFlows, key, webClientId, uids)
                }
                else {
                    // if a new holder holds a selected cred like AutofillCredentialHolder
                    val currCredential = AutofillCredentialHolder.currentCredential
                    if (currCredential != null) {
                        postCredential(requestFlows, key, webClientId, currCredential)
                    } else {
                        // waiting for user s selection
                        toErrorResponse(HttpStatusCode.NotFound, "no user selection")
                    }
                }
            }
            CredentialRequestState.Fulfilled -> {
                webClientRequestIdentifier = null
                return toErrorResponse(HttpStatusCode.Conflict, "still provided")
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
                    "wants to fetch credential with name '$name'.",
                    shortenedFingerprint,
                    "Returning credential with name '$name' ...",
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
                        shortenedFingerprint
                    )
                }
                else {
                    toastText(
                        requestFlows.getLifeCycleActivity(),
                        "Requested credential to synchronise not found."
                    )
                    webClientCredentialRequestState = CredentialRequestState.Denied
                }
            }
        }
    }

    private fun startFetchCredentialForUidsFlow(
        requestFlows: RequestFlows,
        webExtension: EncWebExtension,
        webClientTitle: String,
        webClientId: String,
        shortenedFingerprint: String,
    ) {

        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            "wants to sync all local credentials.",
            shortenedFingerprint,
            "Returning all local credentials ...",
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
        shortenedFingerprint: String
    ) {
        val domain = extractDomain(website, withTld = true)
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            "wants to fetch credential for '$domain'.",
            shortenedFingerprint,
            "Select the credential for '$domain' to fulfill the request.",
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
            "wants to create a new credential for '$domain'.",
            shortenedFingerprint,
            "Create a new credential for '$domain' to fulfill the request.",
            showByPassSnackbar = false,
            denyOnDismiss = false
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
        shortenedFingerprint: String
    ) {
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            "wants to unlock the client vault.",
            shortenedFingerprint,
            "Unlocking client vault ...",
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
        shortenedFingerprint: String
    ) {
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            "wants to ask for any credential. Please select one.",
            shortenedFingerprint,
            "Select a credential to fulfill the request.",
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
        shortenedFingerprint: String
    ) {
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            "wants to fetch for multiple credentials",
            shortenedFingerprint,
            "Select all credentials to fetch and press the Action-button.",
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
        shortenedFingerprint: String
    ) {
        showClientRequest(
            requestFlows,
            webExtension,
            webClientTitle,
            webClientId,
            "wants to fetch for ALL credentials!",
            shortenedFingerprint,
            "Fetching all credentials...",
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
        shortenedFingerprint: String,
        userActionText: String,
        denyOnDismiss: Boolean = true,
        showByPassSnackbar: Boolean = true,
        acceptHandler: () -> Unit,
    ) {
        if (webExtension.bypassIncomingRequests) {
            webClientCredentialRequestState = CredentialRequestState.Accepted
            acceptHandler()

            if (showByPassSnackbar) {
                showBypassSnackbar(
                    requestFlows,
                    webClientTitle,
                    webClientId,
                    details,
                    shortenedFingerprint,
                    webExtension,
                    denyOnDismiss
                )
            }
        } else {
            showAcceptBottomSheet(
                requestFlows.getLifeCycleActivity(),
                webClientTitle,
                webClientId,
                details,
                shortenedFingerprint,
                webExtension,
            )
            {
                acceptHandler()
                showUserActionSnackbar(
                    requestFlows,
                    userActionText,
                    denyOnDismiss
                )
            }
        }
    }

    private fun showAcceptBottomSheet(
        activity: SecureActivity,
        webClientTitle: String,
        webClientId: String,
        details: String,
        shortenedFingerprint: String,
        webExtension: EncWebExtension,
        acceptHandler: () -> Unit,
    ) {
        ServerRequestBottomSheet(
            activity,
            webClientTitle = webClientTitle,
            webClientId = webClientId,
            webRequestDetails = details,
            fingerprint = shortenedFingerprint,
            denyHandler = { allowBypass ->
                webExtension.bypassIncomingRequests = allowBypass
                activity.webExtensionViewModel.save(webExtension, activity)

                webClientCredentialRequestState = CredentialRequestState.Denied
                toastText(activity, "Request denied")
            },
            acceptHandler = { allowBypass ->
                webExtension.bypassIncomingRequests = allowBypass
                activity.webExtensionViewModel.save(webExtension, activity)

                webClientCredentialRequestState = CredentialRequestState.Accepted

                acceptHandler()
            }
        ).show()
    }

    fun showUserActionSnackbar(
        requestFlows: RequestFlows,
        text: String,
        denyOnDismiss: Boolean
    ) {
        serverSnackbar?.dismiss()
        serverSnackbar = Snackbar.make(
            requestFlows.getRootView(),
            text,
            SERVER_REQUEST_SNACKBAR_DURATION
        )
            .setAction("Cancel request") {
                webClientCredentialRequestState = CredentialRequestState.Denied
                requestFlows.resetUi()
                toastText(requestFlows.getLifeCycleActivity(), "Request denied")

            }
            .setTextMaxLines(7)
            .addCallback(object : BaseCallback<Snackbar>() {
                override fun onDismissed(bar: Snackbar, event: Int) {
                    requestFlows.resetUi()
                    requestFlows.getRootView().updatePadding(bottom = 0)

                    credentialSelectState = MultipleCredentialSelectState.NONE

                    if (denyOnDismiss && webClientCredentialRequestState.isProgressing) {
                        webClientCredentialRequestState = CredentialRequestState.Denied
                        toastText(
                            requestFlows.getLifeCycleActivity(),
                            "Request denied"
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
        denyOnDismiss: Boolean
    ) {

        val span =
            SpannableString("$webClientTitle ($webClientId) $details Swipe to cancel. Fingerprint: $shortenedFingerprint")

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
            .setAction("Deny and revoke bypass") {
                webClientCredentialRequestState = CredentialRequestState.Denied
                webExtension.bypassIncomingRequests = false
                requestFlows.getLifeCycleActivity().webExtensionViewModel.save(webExtension, requestFlows.getLifeCycleActivity())
                requestFlows.resetUi()
               // searchItem?.collapseActionView()
                toastText(requestFlows.getLifeCycleActivity(), "Request denied and bypass revoked")

            }
            .setTextMaxLines(7)
            .addCallback(object : BaseCallback<Snackbar>() {
                override fun onDismissed(bar: Snackbar, event: Int) {
                    requestFlows.resetUi()
                    requestFlows.getRootView().updatePadding(bottom = 0)


                    if (denyOnDismiss && webClientCredentialRequestState.isProgressing) {
                        webClientCredentialRequestState = CredentialRequestState.Denied
                        toastText(requestFlows.getLifeCycleActivity(), "Request denied")
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

        val clientKey = SecretService.secretKeyToKey(key, Key(webClientId.toByteArray()))

        val response = JSONObject()

        response.put("credential", responseCredential)
        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        webClientCredentialRequestState = CredentialRequestState.Fulfilled

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            toastText(requestFlows.getLifeCycleActivity(), "Credential '$name' posted")
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


        val clientKey = SecretService.secretKeyToKey(key, Key(webClientId.toByteArray()))

        response.put("credentials", responseCredentials)
        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        webClientCredentialRequestState = CredentialRequestState.Fulfilled

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            requestFlows.stopCredentialSelectionMode()

            toastText(requestFlows.getLifeCycleActivity(), "Selected credentials posted")
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
            .filter { !it.isObfuscated }
            .forEach {
                val (_, responseCredential) = mapCredential(key, it, deobfuscate = true)
                responseCredentials.put(responseCredential)
            }

        val clientKey = SecretService.secretKeyToKey(key, Key(webClientId.toByteArray()))


        response.put("credentials", responseCredentials)
        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        webClientCredentialRequestState = CredentialRequestState.Fulfilled

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            toastText(requestFlows.getLifeCycleActivity(), "All credentials posted")
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
            .filter { !it.isObfuscated }
            .forEach {
                val (_, responseCredential) = mapCredential(key, it, deobfuscate = true)
                responseCredentials.put(responseCredential)
            }

        val clientKey = SecretService.secretKeyToKey(key, Key(webClientId.toByteArray()))


        response.put("credentials", responseCredentials)
        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        webClientCredentialRequestState = CredentialRequestState.Fulfilled

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            toastText(requestFlows.getLifeCycleActivity(), "Credentials posted")
        }

        return Pair(HttpStatusCode.OK, response)
    }

    private fun postClientKey(
        requestFlows: RequestFlows,
        key: SecretKeyHolder,
        webClientId: String,
    ): Pair<HttpStatusCode, JSONObject> {

        val clientKey = SecretService.secretKeyToKey(key, Key(webClientId.toByteArray()))
        val response = JSONObject()

        response.put("clientKey", clientKey.toBase64String())

        clientKey.clear()

        webClientRequestIdentifier = null
        webClientCredentialRequestState = CredentialRequestState.Fulfilled

        CoroutineScope(Dispatchers.Main).launch {
            serverSnackbar?.dismiss()
            toastText(requestFlows.getLifeCycleActivity(), "Local vault unlocked")
        }

        return Pair(HttpStatusCode.OK, response)
    }

    private fun mapCredential(
        key: SecretKeyHolder,
        credential: EncCredential,
        deobfuscate: Boolean
    ): Pair<String, JSONObject> {
        val password = SecretService.decryptPassword(key, credential.password)
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



}