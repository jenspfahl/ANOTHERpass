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
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec


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
            val sessionKeyBase64 = splitted[1]
            val clientPubKeyFingerprint = splitted[2]

            Log.i("HTTP", "received clientPubKeyFingerprint=$clientPubKeyFingerprint")
            Log.i("HTTP", "received sessionKeyBase64=$sessionKeyBase64")

            if (webClientId.isNullOrBlank() || sessionKeyBase64.isNullOrBlank() || clientPubKeyFingerprint.isNullOrBlank()) {
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
                val encTempSessionKey = SecretService.encryptCommonString(key, sessionKeyBase64)
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
                        encTempSessionKey, // we borrow this field in the linking phase
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

    override fun callHttpRequestHandler(
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
                return Pair(HttpStatusCode.InternalServerError, HttpServer.toErrorJson("invalid action"))
            }
            else{
                Log.d("HTTP", "Validate client pubkey")

                /*
                action: "link_app",
                clientPublicKey: clientPublicKeyAsPEM,
                configuredServer: server
                */
                val clientPubKeyAsJWK= message.getString("clientPublicKey")
                val serverName= message.getString("configuredServer") //TODO check and store this later

                val clientPubKeyAsJSON = JSONObject(clientPubKeyAsJWK)
                val nBase64 = clientPubKeyAsJSON.getString("n")
                val eBase64 = clientPubKeyAsJSON.getString("e")
                Log.d("HTTP", "clientPubKey.n=$nBase64")
                Log.d("HTTP", "clientPubKey.e=$eBase64")
                //val n = Base64.decode(nBase64, Base64.URL_SAFE)

                val nHashed = nBase64.toByteArray().sha256()
                val fingerprintToHex = nHashed.toHex()
                Log.d("HTTP", "fingerprintToHex=$fingerprintToHex")

                val knownClientPubKeyFingerprintHex = SecretService.decryptCommonString(key, webExtension.extensionPublicKey)

                Log.d("HTTP", "knownClientPubKeyFingerprintHex=$knownClientPubKeyFingerprintHex")

                if (fingerprintToHex != knownClientPubKeyFingerprintHex) {
                    Log.e("HTTP", "wrong fingerprint")
                    return Pair(HttpStatusCode.BadRequest, HttpServer.toErrorJson("fingerprint missmatch"))
                }
                Log.i("HTTP", "client pubkey approved")


                // generate shared base key
                val sharedBaseKey = SecretService.generateRandomKey(16, this)

                webExtension.sharedBaseKey = SecretService.encryptKey(key, sharedBaseKey)
                webExtension.extensionPublicKey = SecretService.encryptCommonString(key, clientPubKeyAsJWK)
                webExtensionViewModel.save(webExtension, this)


                // generate and store server pubkey //TODO block ui since it takes time ...
                val serverKeyPair = SecretService.generateRsaKeyPair(webExtension.getClientPubKeyAlias(), this)
                val kf = KeyFactory.getInstance("RSA")
                val serverPublicKey = kf.getKeySpec(serverKeyPair.public, RSAPublicKeySpec::class.java)
                val modulus = serverPublicKey.modulus
                val exponent = serverPublicKey.publicExponent

                val nServerBase64 = Base64.encodeToString(normalizeToLength(modulus.toByteArray(), 512), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val eServerBase64 = Base64.encodeToString(normalizeToLength(exponent.toByteArray(), 512), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val sharedBaseKeyBase64 = Base64.encodeToString(sharedBaseKey.toByteArray(), Base64.DEFAULT or Base64.NO_WRAP or Base64.NO_PADDING)

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
        return Pair(HttpStatusCode.Forbidden, HttpServer.toErrorJson("locked"))
    }

    private fun normalizeToLength(bytes: ByteArray, length: Int): ByteArray {
        val offset = bytes.size - length
        if (offset > 0) {
            return bytes.copyOfRange(offset, bytes.size)
        }
        else {
            return bytes
        }
    }

}