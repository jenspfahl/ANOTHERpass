package de.jepfa.yapm.ui.webextension

import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.text.format.Formatter
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.importread.ReadActivityBase
import de.jepfa.yapm.util.observeOnce
import de.jepfa.yapm.util.toastText


class AddWebExtensionActivity : ReadActivityBase() {

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

            if (webClientId == null) {
                toastText(this, "Please scan the QR code first to proceed")
                return@setOnClickListener
            }


            // await link confirmation
            masterSecretKey?.let { key ->

                val title = SecretService.encryptCommonString(key, titleTextView.text.toString())

                webExtensionViewModel.allWebExtensions.observeOnce(this) { webExtensions ->

                    val existingWebExtension = webExtensions
                        .find { SecretService.decryptCommonString(key, it.webClientId) == webClientId }

                    if (existingWebExtension == null) {
                        toastText(this, "Something went wrong")
                        return@observeOnce
                    }
                    if (!existingWebExtension.linked)  {
                        toastText(this, "Please proceed on the extension first!")
                        return@observeOnce
                    }

                    // update title if changed meanwhile by the user
                    existingWebExtension.title = title


                    webExtensionViewModel.save(existingWebExtension, this)

                    toastText(this, "Device linked")

                    finish()
                }
            }
        }
    }


    override fun lock() {
        finish()
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

            //Log.i("HTTP", "received sessionKeyBase64=$sessionKeyBase64")
            Log.i("HTTP", "received clientPubKeyFingerprint=$clientPubKeyFingerprint")

            if (webClientId.isNullOrBlank() || sessionKeyBase64.isNullOrBlank() || clientPubKeyFingerprint.isNullOrBlank()) {
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
                val encClientPublicKey = SecretService.encryptCommonString(key, "$sessionKeyBase64:$clientPubKeyFingerprint")

                // save unlinked extension, the HttpServer will complete it once the user proceeds in the extension ...
                webExtensionViewModel.allWebExtensions.observeOnce(this) { webExtensions ->

                    // find one with the same webClientId --> will be overwritten
                    val existingWebExtension = webExtensions
                        .find { SecretService.decryptCommonString(key, it.webClientId) == webClientId }

                    var id: Int? = null
                    if (existingWebExtension != null) {
                        id = existingWebExtension.id
                    }

                    val webExtension = EncWebExtension(
                        id,
                        encWebClientId,
                        title,
                        encClientPublicKey,
                        linked = false,
                        enabled = true,
                        bypassIncomingRequests = false,
                        lastUsedTimestamp = null
                    )

                    webExtensionViewModel.save(webExtension, this)
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

}