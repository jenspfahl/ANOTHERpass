package de.jepfa.yapm.ui.webextension

import android.graphics.Typeface
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.getIntExtra
import de.jepfa.yapm.util.toastText


class AddWebExtensionActivity : SecureActivity() {

    private lateinit var titleTextView: TextView
    private lateinit var qrCodeScannerImageView: ImageView
    private lateinit var webClientIdTextView: TextView
    private lateinit var serverAddressTextView: TextView

    private var scannedLoad: String? = null

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_web_extension)

        titleTextView = findViewById(R.id.edit_web_extension_title)
        qrCodeScannerImageView = findViewById(R.id.web_extension_scan_qrcode)
        webClientIdTextView = findViewById(R.id.web_extension_client_id)
        serverAddressTextView = findViewById(R.id.web_extension_server_address)


        serverAddressTextView.text = getHostAddress()

        qrCodeScannerImageView.setOnClickListener {
            qrCodeScannerImageView.visibility = ViewGroup.GONE
            webClientIdTextView.visibility = ViewGroup.VISIBLE
            webClientIdTextView.text = "DUMM-MMMY"
            scannedLoad = "stuff"
        }

        val saveButton: Button = findViewById(R.id.button_save)
        saveButton.setOnClickListener {

            if (TextUtils.isEmpty(titleTextView.text)) {
                titleTextView.error = getString(R.string.error_field_required)
                titleTextView.requestFocus()
                return@setOnClickListener
            }

            if (scannedLoad == null) {
                toastText(this, "Please scan the QR code first to proceed")
                return@setOnClickListener
            }

            masterSecretKey?.let { key ->

                val title = SecretService.encryptCommonString(key, titleTextView.text.toString())
                val webClientId = SecretService.encryptCommonString(key, webClientIdTextView.text.toString())
                val clientPublicKey = SecretService.encryptCommonString(key, "Client PubK")
                val serverKeyPairAlias = SecretService.encryptCommonString(key, "Server PK Alias")

                val webExtension = EncWebExtension(null,
                    webClientId,
                    title,
                    clientPublicKey,
                    serverKeyPairAlias,
                    linked = true,
                    enabled = true,
                    lastUsedTimestamp = null
                )

                webExtensionViewModel.insert(webExtension, this)

                toastText(this, "Device linked")
            }

            finish()
        }
    }


    override fun lock() {
        finish()
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