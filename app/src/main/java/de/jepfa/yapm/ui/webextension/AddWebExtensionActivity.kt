package de.jepfa.yapm.ui.webextension

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.net.HttpServer
import de.jepfa.yapm.service.net.HttpServer.toErrorResponse
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.ServerRequestBottomSheet
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.importread.ReadActivityBase
import de.jepfa.yapm.usecase.webextension.DeleteWebExtensionUseCase
import de.jepfa.yapm.util.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


class AddWebExtensionActivity : ReadActivityBase(), HttpServer.HttpCallback {

    private var currentLockTimeout: Int = 0
    private var currentLogoutTimeout: Int = 0
    private lateinit var progressBar: ProgressBar
    private var webExtension: EncWebExtension? = null
    private var webClientId: String? = null
    private lateinit var saveButton: Button
    private lateinit var titleTextView: EditText
    private lateinit var qrCodeScannerImageView: ImageView
    private lateinit var webClientIdTextView: TextView
    private lateinit var serverAddressTextView: TextView


    init {
        onlyQrCodeScan = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentLockTimeout = PreferenceService.getAsInt(PreferenceService.PREF_LOCK_TIMEOUT, this)
        currentLogoutTimeout = PreferenceService.getAsInt(PreferenceService.PREF_LOGOUT_TIMEOUT, this)

        // give the user at max 15 minutes to hassle with the extension
        Session.setTimeouts(
            currentLockTimeout.coerceAtLeast(15),
            currentLogoutTimeout.coerceAtLeast(15),
        )

        titleTextView = findViewById(R.id.edit_web_extension_title)
        qrCodeScannerImageView = findViewById(R.id.imageview_scan_qrcode)
        webClientIdTextView = findViewById(R.id.web_extension_client_id)
        serverAddressTextView = findViewById(R.id.web_extension_server_address)

        serverAddressTextView.text = HttpServer.getHostNameOrIpAndHandle(this, emphasiseHandle = true) {
            serverAddressTextView.text = it
        }

        progressBar = getProgressBar()!!


        saveButton = findViewById(R.id.button_save)
        saveButton.setOnClickListener {

            if (TextUtils.isEmpty(titleTextView.text) || titleTextView.text.isBlank()) {
                titleTextView.error = getString(R.string.error_field_required)
                titleTextView.requestFocus()
                return@setOnClickListener
            }

            if (!hasQrCodeScanned()) {
                toastText(this, getString(R.string.scan_qr_code_first))
                return@setOnClickListener
            }

            if (webExtension == null) {
                toastText(this, R.string.something_went_wrong)
                return@setOnClickListener
            }
            else if (!webExtension!!.linked)  {
                toastText(this, getString(R.string.please_proceed_with_the_extension))
                return@setOnClickListener
            }

            toastText(this, getString(R.string.device_linked))

            finish()

        }

        HttpServer.linkHttpCallback = this
    }

    private fun hasQrCodeScanned() = webClientId != null


    override fun onBackPressed() {
        if (hasQrCodeScanned()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.cancel_linking_device_title, webClientId))
                .setMessage(getString(R.string.cancel_linking_device_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    removeWebExtension()

                    super.onBackPressed()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (hasQrCodeScanned()) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.cancel_linking_device_title, webClientId))
                    .setMessage(getString(R.string.cancel_linking_device_message))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        removeWebExtension()

                        val upIntent = Intent(this.intent)
                        navigateUpTo(upIntent)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
            }
            else {
                super.onOptionsItemSelected(item)
            }

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun removeWebExtension() {
        webExtension?.let { current ->
            UseCaseBackgroundLauncher(DeleteWebExtensionUseCase)
                .launch(this, current)
        }
    }

    override fun isAllowedToScanQrCode(): Boolean {
        hideKeyboard(titleTextView)
        if (TextUtils.isEmpty(titleTextView.text) || titleTextView.text.isBlank()) {
            titleTextView.error = getString(R.string.error_field_required)
            titleTextView.requestFocus()
            return false
        }
        return true
    }

    override fun lock() {
        finish()
    }

    override fun onDestroy() {
        HttpServer.linkHttpCallback = null
        Session.setTimeouts(
            currentLockTimeout,
            currentLogoutTimeout,
        )
        super.onDestroy()
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_add_web_extension
    }

    override fun handleScannedData(scanned: String) {
        val splitted = scanned.split(":")
        if (splitted.size != 3) {
            toastText(this, getString(R.string.unknown_qr_code))
        }
        else {
            webClientId = splitted[0]
            val sessionKeyBase64 = splitted[1]
            val clientPubKeyFingerprint = splitted[2]

            Log.i("HTTP", "received clientPubKeyFingerprint=$clientPubKeyFingerprint")
            Log.i("HTTP", "received sessionKeyBase64=$sessionKeyBase64")

            if (webClientId.isNullOrBlank()
                || sessionKeyBase64.isNullOrBlank()
                || clientPubKeyFingerprint.isNullOrBlank()
                || !HttpServer.checkWebClientIdFormat(webClientId!!)) {
                webClientId = null
                toastText(this, R.string.unknown_qr_code)
                return
            }

            val sessionKey = Key(Base64.decode(sessionKeyBase64, Base64.DEFAULT))
            Log.i("HTTP", "received sessionKey=${sessionKey.debugToString()}")

            qrCodeScannerImageView.visibility = ViewGroup.GONE
            webClientIdTextView.visibility = ViewGroup.VISIBLE
            webClientIdTextView.text = webClientId
            titleTextView.isEnabled = false

            masterSecretKey?.let { key ->

                val title = SecretService.encryptCommonString(key, titleTextView.text.toString())
                val encWebClientId = SecretService.encryptCommonString(key, webClientId!!)
                // this field contains the QR code payload in this phase
                val encSessionKey = SecretService.encryptKey(key, sessionKey)
                val encClientPublicKeyFingerprint = SecretService.encryptCommonString(key, clientPubKeyFingerprint)

                // save unlinked extension, the HttpServer will complete it once the user proceeds in the extension ...
                webExtensionViewModel.allWebExtensions.observeOnce(this) { webExtensions ->

                    // find one with the same webClientId --> will be overwritten
                    val existingWebExtension = webExtensions
                        .find { SecretService.decryptCommonString(key, it.webClientId) == webClientId }

                    var id: Int? = null
                    if (existingWebExtension != null) {
                        if (existingWebExtension.linked) {
                            finish()
                            toastText(this, getString(R.string.relink_device_not_possible))
                            return@observeOnce
                        }
                        id = existingWebExtension.id
                    }

                    webExtension = EncWebExtension(
                        id,
                        encWebClientId,
                        title,
                        encClientPublicKeyFingerprint, // we borrow this field in the linking phase
                        encSessionKey, // we borrow this field in the linking phase
                        linked = false,
                        enabled = true,
                        bypassIncomingRequests = false,
                        lastUsedTimestamp = null
                    )

                    webExtensionViewModel.save(webExtension!!, this)
                }
            }
        }
    }

    override fun handleHttpRequest(
        action: HttpServer.Action,
        webClientId: String,
        webExtension: EncWebExtension,
        message: JSONObject,
        origin: RequestConnectionPoint
    ): Pair<HttpStatusCode, JSONObject> {

        masterSecretKey?.let { key ->
            val providedWebClientId = SecretService.decryptCommonString(key, webExtension.webClientId)
            if (providedWebClientId != webClientId || action != HttpServer.Action.LINKING) {
                Log.e("HTTP", "Programming error")
                toastText(this, R.string.something_went_wrong)
                return toErrorResponse(HttpStatusCode.InternalServerError, "invalid action or unexpected web client")
            }
            else{
                Log.d("HTTP", "Validate client pubkey")

                val clientPubKeyAsJWK = message.getJSONObject("clientPublicKey")

                val nBase64 = clientPubKeyAsJWK.getString("n")
                val eBase64 = clientPubKeyAsJWK.getString("e")
                Log.d("HTTP", "clientPubKey.n=$nBase64")
                Log.d("HTTP", "clientPubKey.e=$eBase64")
                Log.d("HTTP", "clientPubKey as bytes=${Base64.decode(nBase64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING).contentToString()}")

                val nHashed = nBase64.toByteArray().sha256()
                val fingerprintToHex = nHashed.toHex()

                val knownClientPubKeyFingerprintHex = SecretService.decryptCommonString(key, webExtension.extensionPublicKey)
                Log.d("HTTP", "fingerprintToHex=$fingerprintToHex")
                Log.d("HTTP", "knownClientPubKeyFingerprintHex=$knownClientPubKeyFingerprintHex")

                if (fingerprintToHex != knownClientPubKeyFingerprintHex) {
                    Log.e("HTTP", "wrong fingerprint")
                    return toErrorResponse(HttpStatusCode.BadRequest,"fingerprint mismatch")
                }
                Log.i("HTTP", "client public key approved")

                val remoteVaultId = message.optString("vaultId")
                Log.d("HTTP", "remoteVaultId=$remoteVaultId")
                if (remoteVaultId.isNotBlank() && remoteVaultId != SaltService.getVaultId(this)) {
                    Log.e("HTTP", "relink vault id mismatch")
                    return toErrorResponse(HttpStatusCode.BadRequest,"relink vault id mismatch")
                }

                val sharedBaseKey = HttpServer.createSymmetricKey(this)

                webExtension.sharedBaseKey = SecretService.encryptKey(key, sharedBaseKey)
                webExtension.extensionPublicKey = SecretService.encryptCommonString(key, clientPubKeyAsJWK.toString())
                webExtensionViewModel.save(webExtension, this)


                // generate and store server RSA key pair
                CoroutineScope(Dispatchers.Main).launch {
                    showProgressBar()
                }

                val serverKeyPair = SecretService.generateRsaKeyPair(webExtension.getServerKeyPairAlias(), this, workaroundMode = true)
                val serverPublicKeyData = SecretService.getRsaPublicKeyData(serverKeyPair.public)

                val nServerBase64 = Base64.encodeToString(serverPublicKeyData.first, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val eServerBase64 = Base64.encodeToString(serverPublicKeyData.second, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val sharedBaseKeyBase64 = Base64.encodeToString(sharedBaseKey.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                sharedBaseKey.clear()

                Log.d("HTTP", "sharedBaseKeyBase64=$sharedBaseKeyBase64")

                val jwk = JSONObject()
                jwk.put("n", nServerBase64)
                jwk.put("e", eServerBase64)

                val nServerHashed = nServerBase64.toByteArray().sha256()
                val shortenedServerPubKeyFingerprint = Key(nServerHashed).toShortenedFingerprint()

                val response = JSONObject()
                response.put("serverPubKey", jwk)
                response.put("sharedBaseKey", sharedBaseKeyBase64)
                response.put("linkedVaultId", SaltService.getVaultId(this))

                val webClientTitle = SecretService.decryptCommonString(key, webExtension.title)


                CoroutineScope(Dispatchers.Main).launch {
                    hideProgressBar()


                    ServerRequestBottomSheet(
                        this@AddWebExtensionActivity,
                        webClientTitle = webClientTitle,
                        webClientId = webClientId,
                        webRequestDetails = getString(R.string.action_link_device_details),
                        fingerprint = shortenedServerPubKeyFingerprint,
                        hideBypassFlag = true,
                        denyHandler = {_ ->
                            removeWebExtension()
                            val upIntent = Intent(this@AddWebExtensionActivity.intent)
                            navigateUpTo(upIntent)

                            toastText(this@AddWebExtensionActivity, R.string.request_denied)
                        },
                        acceptHandler = { allowBypass ->
                            webExtension.linked = true
                            webExtension.bypassIncomingRequests = allowBypass
                            webExtensionViewModel.save(webExtension!!, this@AddWebExtensionActivity)

                            val upIntent = Intent(this@AddWebExtensionActivity.intent)
                            navigateUpTo(upIntent)

                            toastText(this@AddWebExtensionActivity, R.string.device_linked)
                        }
                    ).show()
                }

                Log.d("HTTP", "link response=" + response.toString(4))

                return Pair(HttpStatusCode.OK, response)
            }
        }
        // no key / logged out
        return toErrorResponse(HttpStatusCode.Forbidden, "locked")
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }


    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
        window?.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

}