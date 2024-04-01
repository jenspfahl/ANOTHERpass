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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.service.net.HttpServer
import de.jepfa.yapm.service.net.HttpServer.toErrorResponse
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.importread.ReadActivityBase
import de.jepfa.yapm.usecase.webextension.DeleteWebExtensionUseCase
import de.jepfa.yapm.util.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


class AddWebExtensionActivity : ReadActivityBase(), HttpServer.Listener {

    private lateinit var progressBar: ProgressBar
    private var webExtension: EncWebExtension? = null
    private var webClientId: String? = null
    private lateinit var saveButton: Button
    private lateinit var titleTextView: TextView
    private lateinit var qrCodeScannerImageView: ImageView
    private lateinit var webClientIdTextView: TextView
    private lateinit var serverAddressTextView: TextView


    init {
        onlyQrCodeScan = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleTextView = findViewById(R.id.edit_web_extension_title)
        qrCodeScannerImageView = findViewById(R.id.imageview_scan_qrcode)
        webClientIdTextView = findViewById(R.id.web_extension_client_id)
        serverAddressTextView = findViewById(R.id.web_extension_server_address)

        serverAddressTextView.text = HttpServer.getHostNameOrIp(this) {
            serverAddressTextView.text = it
        }

        progressBar = getProgressBar()!!


        saveButton = findViewById(R.id.button_save)
        saveButton.setOnClickListener {

            if (TextUtils.isEmpty(titleTextView.text)) {
                titleTextView.error = getString(R.string.error_field_required)
                titleTextView.requestFocus()
                return@setOnClickListener
            }

            if (!hasQrCodeScanned()) {
                toastText(this, "Please scan the QR code first to proceed")
                return@setOnClickListener
            }

            if (webExtension == null) {
                toastText(this, "Something went wrong")
                return@setOnClickListener
            }
            else if (!webExtension!!.linked)  {
                toastText(this, "Please proceed on the extension first!")
                return@setOnClickListener
            }

            toastText(this, "Device linked")

            finish()

        }

        HttpServer.linkListener = this
    }

    private fun hasQrCodeScanned() = webClientId != null


    override fun onBackPressed() {
        if (hasQrCodeScanned()) {
            AlertDialog.Builder(this)
                .setTitle("Cancel linking device $webClientId")
                .setMessage("Going back will cancel the current linking, sure?")
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
                    .setTitle("Cancel linking device $webClientId")
                    .setMessage("Going back will cancel the current linking, sure?")
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
        if (TextUtils.isEmpty(titleTextView.text)) {
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
        HttpServer.linkListener = null
        super.onDestroy()
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_add_web_extension
    }

    override fun handleScannedData(scanned: String) {
        val splitted = scanned.split(":")
        if (splitted.size != 3) {
            toastText(this, "Unknown QR code content")
        }
        else {
            webClientId = splitted[0]
            val sessionKeyBase64 = splitted[1]
            val clientPubKeyFingerprint = splitted[2]

            Log.i("HTTP", "received clientPubKeyFingerprint=$clientPubKeyFingerprint")
            Log.i("HTTP", "received sessionKeyBase64=$sessionKeyBase64")

            if (webClientId.isNullOrBlank() || sessionKeyBase64.isNullOrBlank() || clientPubKeyFingerprint.isNullOrBlank()) {
                webClientId = null
                toastText(this, "Wrong QR code")
                return
            }

            val sessionKey = Key(Base64.decode(sessionKeyBase64, Base64.DEFAULT))
            Log.i("HTTP", "received sessionKey=${sessionKey.debugToString()}")

            qrCodeScannerImageView.visibility = ViewGroup.GONE
            webClientIdTextView.visibility = ViewGroup.VISIBLE
            webClientIdTextView.text = webClientId

            masterSecretKey?.let { key ->

                val title = SecretService.encryptCommonString(key, titleTextView.text.toString())
                val encWebClientId = SecretService.encryptCommonString(key, webClientId!!)
                // this field contains the QR code payload in this phase
                val encSessionKey = SecretService.encryptKey(key, sessionKey)
                val encClientPublicKey = SecretService.encryptCommonString(key, clientPubKeyFingerprint)

                // save unlinked extension, the HttpServer will complete it once the user proceeds in the extension ...
                webExtensionViewModel.allWebExtensions.observeOnce(this) { webExtensions ->

                    // find one with the same webClientId --> will be overwritten
                    val existingWebExtension = webExtensions
                        .find { SecretService.decryptCommonString(key, it.webClientId) == webClientId }

                    var id: Int? = null
                    if (existingWebExtension != null) {
                        id = existingWebExtension.id
                    }

                    webExtension = EncWebExtension(
                        id,
                        encWebClientId,
                        title,
                        encClientPublicKey,
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
        message: JSONObject
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
                    return toErrorResponse(HttpStatusCode.BadRequest,"fingerprint missmatch")
                }
                Log.i("HTTP", "client public key approved")


                // generate shared base key
                val sharedBaseKey = SecretService.generateRandomKey(16, this)

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

            
                CoroutineScope(Dispatchers.Main).launch {
                    hideProgressBar()

                    AlertDialog.Builder(this@AddWebExtensionActivity)
                        .setTitle("Linking device $webClientId")
                        .setMessage("Ensure that the fingerprint shown here is the same as displayed in the linked browser: " + shortenedServerPubKeyFingerprint)
                        .setPositiveButton("Yes, same!") { _, _ ->

                            webExtension.linked = true
                            webExtensionViewModel.save(webExtension!!, this@AddWebExtensionActivity)

                            val upIntent = Intent(this@AddWebExtensionActivity.intent)
                            navigateUpTo(upIntent)

                            toastText(this@AddWebExtensionActivity, "Device linked!")
                        }
                        .setNegativeButton("No, different!") {_, _ ->
                            removeWebExtension()
                            val upIntent = Intent(this@AddWebExtensionActivity.intent)
                            navigateUpTo(upIntent)

                            toastText(this@AddWebExtensionActivity, "Not linked!")
                        }
                        .show()
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