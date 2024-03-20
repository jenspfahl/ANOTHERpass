package de.jepfa.yapm.ui.webextension

import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
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


        serverAddressTextView.text = getHostAddress()

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
            val baseKeyBase64 = splitted[1]
            val clientPubKeyFingerprint = splitted[2]

            Log.i("HTTP", "received clientPubKeyFingerprint=$clientPubKeyFingerprint")
            Log.i("HTTP", "received BaseKeyBase64=$baseKeyBase64")

            if (webClientId.isNullOrBlank() || baseKeyBase64.isNullOrBlank() || clientPubKeyFingerprint.isNullOrBlank()) {
                webClientId = null
                toastText(this, "Wrong QR code")
                return
            }

            qrCodeScannerImageView.visibility = ViewGroup.GONE
            webClientIdTextView.visibility = ViewGroup.VISIBLE
            webClientIdTextView.text = webClientId

            masterSecretKey?.let { key ->

                val title = SecretService.encryptCommonString(key, titleTextView.text.toString())
                val encWebClientId = SecretService.encryptCommonString(key, webClientId!!)
                // this field contains the QR code payload in this phase
                val encBaseKey = SecretService.encryptCommonString(key, baseKeyBase64)
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
                        encBaseKey, // we borrow this field in the linking phase
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

    private fun getHostAddress(): String {
        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        val ipAddress =
            Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        val deviceName = getDeviceName()

        return if (deviceName != null) {
            "$ipAddress ($deviceName)"
        } else {
            "$ipAddress"
        }
    }


    private fun getDeviceName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            return Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
        }
        return null
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


                // generate and store server pubkey //TODO block ui since it takes time ... --> move thid into a UseCase
                val serverKeyPair = SecretService.generateRsaKeyPair(webExtension.getClientPubKeyAlias(), this)
                val serverPublicKeyData = SecretService.getRsaPublicKeyData(serverKeyPair.public)

                val nServerBase64 = Base64.encodeToString(serverPublicKeyData.first, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val eServerBase64 = Base64.encodeToString(serverPublicKeyData.second, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val sharedBaseKeyBase64 = Base64.encodeToString(sharedBaseKey.toByteArray(), Base64.DEFAULT or Base64.NO_WRAP or Base64.NO_PADDING)
                sharedBaseKey.clear()

                Log.d("HTTP", "sharedBaseKeyBase64=$sharedBaseKeyBase64")

                val jwk = JSONObject()
                jwk.put("n", nServerBase64)
                jwk.put("e", eServerBase64)

                val nServerHashed = nServerBase64.toByteArray().sha256()
                val serverPubKeyFingerprint = Base64.encodeToString(nServerHashed, Base64.DEFAULT)
                    .replace(Regex("[^a-zA-Z0-9]"), "")
                    .substring(0, 6)
                    .lowercase()

                val shortenedServerPubKeyFingerprint = serverPubKeyFingerprint.substring(0, 2) + "-" + serverPubKeyFingerprint.substring(2, 4) + "-" + serverPubKeyFingerprint.substring(4, 6)

                val response = JSONObject()
                response.put("serverPubKey", jwk)
                response.put("sharedBaseKey", sharedBaseKeyBase64)


                CoroutineScope(Dispatchers.Main).launch {
                    AlertDialog.Builder(this@AddWebExtensionActivity)
                        .setTitle("Linking device $webClientId")
                        .setMessage("Ensure that the fingerprint shown here is the same as displayed in the linked browser: " + shortenedServerPubKeyFingerprint)
                        .setPositiveButton("Yes, same!", null)
                        .setNegativeButton("No, different!", null)
                        .show()
                }

                Log.d("HTTP", "link response=" + response.toString(4))

                return Pair(HttpStatusCode.OK, response)
            }
        }
        // no key / logged out
        return toErrorResponse(HttpStatusCode.Forbidden, "locked")
    }

}