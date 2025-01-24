package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.widget.addTextChangedListener
import com.google.android.material.progressindicator.CircularProgressIndicator
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncCredential.Companion.EXTRA_CREDENTIAL_OTP_DATA
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.otp.OTPAlgorithm
import de.jepfa.yapm.model.otp.OTPConfig
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_ALGORITHM
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_COUNTER
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_DIGITS
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_MODE
import de.jepfa.yapm.model.otp.OTPConfig.Companion.DEFAULT_OTP_PERIOD
import de.jepfa.yapm.model.otp.OTPMode
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.otp.OtpService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.importread.ReadQrCodeOrNfcActivityBase
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.putEncryptedExtra
import de.jepfa.yapm.util.toastText
import java.util.Date


class ConfigOtpActivity : ReadQrCodeOrNfcActivityBase() {

    private val timer = Handler(Looper.getMainLooper())
    private var timerRunner: Runnable? = null


    private var otpToSave: OTPConfig? = null

    private lateinit var otpImage: ImageView
    private lateinit var totpProgressCircle: CircularProgressIndicator
    private lateinit var hotpAdjustCounter: ImageView
    private lateinit var qrCodeScannerImageView: ImageView
    private lateinit var otpModeSelection: AppCompatSpinner
    private lateinit var otpAlgorithmSelection: AppCompatSpinner
    private lateinit var sharedSecretEditText: EditText
    private lateinit var counterOrPeriodEditText: EditText
    private lateinit var digitsEditText: EditText
    private lateinit var counterOrPeriodTextView: TextView
    private lateinit var otpAuthTextView: TextView
    private lateinit var otpValueTextView: TextView

    private var issuer: String? = null
    private var account: String? = null
    private var secret: Key? = null
    private var otpMode = DEFAULT_OTP_MODE
    private var otpAlgorithm = DEFAULT_OTP_ALGORITHM
    private var counter = DEFAULT_OTP_COUNTER
    private var period = DEFAULT_OTP_PERIOD


    init {
        enableBack = true
        onlyQrCodeScan = true

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        qrCodeScannerImageView = findViewById(R.id.imageview_scan_qrcode)
        otpAuthTextView = findViewById(R.id.otpauth_text)
        if (!DebugInfo.isDebug) {
            otpAuthTextView.visibility = View.GONE
        }
        otpValueTextView = findViewById(R.id.otp_value)
        otpImage = findViewById(R.id.otp_image)
        totpProgressCircle = findViewById(R.id.totp_progress_circle)
        hotpAdjustCounter = findViewById(R.id.hotp_adjust_counter)

        otpModeSelection = findViewById(R.id.otp_mode_selection)
        val otpModeSelectionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            OTPMode.entries
                .map { getString(it.uiLabel)})
        otpModeSelection.adapter = otpModeSelectionAdapter
        otpModeSelection.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newOtpMode = OTPMode.entries[position]
                if (otpMode != newOtpMode) {
                    otpMode = newOtpMode

                    updateCounterOrPeriodTextView()
                    updateOtpAuthTextView()
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                otpMode = DEFAULT_OTP_MODE
                updateOtpAuthTextView()
            }
        }
        otpModeSelection.setSelection(otpMode.ordinal)


        otpAlgorithmSelection = findViewById(R.id.otp_algo_selection)
        val otpAlgorithmSelectionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            OTPAlgorithm.entries
                .map { it.uiName})
        otpAlgorithmSelection.adapter = otpAlgorithmSelectionAdapter
        otpAlgorithmSelection.onItemSelectedListener = object: OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                otpAlgorithm = OTPAlgorithm.entries[position]
                updateOtpAuthTextView()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                otpAlgorithm = DEFAULT_OTP_ALGORITHM
                updateOtpAuthTextView()
            }
        }
        otpAlgorithmSelection.setSelection(otpAlgorithm.ordinal)


        counterOrPeriodTextView = findViewById(R.id.text_otp_counter_or_period)
        sharedSecretEditText = findViewById(R.id.edit_otp_shared_secret)
        sharedSecretEditText.addTextChangedListener {
            updateOtpAuthTextView()
        }

        counterOrPeriodEditText = findViewById(R.id.edit_otp_counter_or_period)
        counterOrPeriodEditText.addTextChangedListener { text ->
            updateOtpAuthTextView()
            val value = text.toString().toIntOrNull()
            if (otpMode == OTPMode.HOTP && value != null) {
                counter = value
            }
            if (otpMode == OTPMode.TOTP && value != null) {
                period = value
            }
        }

        digitsEditText = findViewById(R.id.edit_otp_digits)
        digitsEditText.addTextChangedListener {
            updateOtpAuthTextView()
        }
        digitsEditText.setText(DEFAULT_OTP_DIGITS.toString())

        hotpAdjustCounter.setOnClickListener {
            if (otpMode == OTPMode.HOTP) {
                adjustHOTPCounter()
            }
        }

        val saveButton: Button = findViewById(R.id.button_save)
        saveButton.setOnClickListener {

            if (sharedSecretEditText.text.isBlank()) {
                sharedSecretEditText.requestFocus()
                toastText(this, R.string.otp_error_secret_not_in_base32)
                return@setOnClickListener
            }

            if (counterOrPeriodEditText.text.isBlank()) {
                counterOrPeriodEditText.requestFocus()
                toastText(this, R.string.error_field_required)
                return@setOnClickListener
            }

            if (counterOrPeriodEditText.text.toString().toIntOrNull() ?: 0 <= 0) {
                counterOrPeriodEditText.requestFocus()
                toastText(this, R.string.error_value_greater_zero_required)
                return@setOnClickListener
            }

            if (digitsEditText.text.isBlank()) {
                digitsEditText.requestFocus()
                toastText(this, R.string.error_field_required)
                return@setOnClickListener
            }

            if (digitsEditText.text.toString().toIntOrNull() ?: 0 <= 0) {
                digitsEditText.requestFocus()
                toastText(this, R.string.error_value_greater_zero_required)
                return@setOnClickListener
            }

            masterSecretKey?.let { key ->
                otpToSave = createOTPConfigFromCurrentState()
                if (otpToSave != null) {
                    val encOtpData =
                        SecretService.encryptCommonString(key, otpToSave!!.toUri().toString())

                    val data = Intent()
                    data.putEncryptedExtra(EXTRA_CREDENTIAL_OTP_DATA, encOtpData)

                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
                else {
                    toastText(this, getString(R.string.otp_cannot_apply))
                }
            }


        }

        val otpToRestore = savedInstanceState?.getString("OTP")
        if (otpToRestore != null) {
            otpToSave = OTPConfig.fromUri(Uri.parse(otpToRestore))
            otpToSave?.let { otpConf -> updateUIFromOTPConfig(otpConf) }
        }
        else {
            updateCounterOrPeriodTextView(includeValues = false)
            loadOTPFromIntent()
        }


        timerRunner = Runnable {
            val hasChanged = updateCurrentOtpValue()
            timerRunner?.let {
                timer.postDelayed(it, 1000L)
                if (otpMode == OTPMode.TOTP && hasChanged) {
                    startOtpProgressAnimation(isFirst = true)
                }
            }
        }
        val elapsedTimeOfSecond = System.currentTimeMillis() % 1000
        val firstDelay = 1000 - elapsedTimeOfSecond
        timer.postDelayed(timerRunner!!, firstDelay)

        if (otpMode == OTPMode.TOTP) {
            startOtpProgressAnimation(isFirst = true)
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("OTP", this.otpToSave?.toUri()?.toString())
    }

    private fun adjustHOTPCounter() {
        val builder = AlertDialog.Builder(this)
        val view: View = this.layoutInflater.inflate(R.layout.number_picker_dialog, null)
        builder.setView(view)

        builder.setTitle(getString(R.string.otp_update_hotp_counter_title))
        builder.setMessage(getString(R.string.otp_update_hotp_counter_message, counter))
        val picker = view.findViewById<View>(R.id.number_picker) as NumberPicker


        picker.minValue = 1
        picker.maxValue = 99_999_999
        picker.value = counter
        builder
            .setPositiveButton(android.R.string.ok, { dialog, _ ->
                counter = picker.value
                dialog.dismiss()

                counterOrPeriodEditText.setText(counter.toString())
                updateOtpAuthTextView()
            })
            .setNegativeButton(android.R.string.cancel, { dialog, _ ->
                dialog.dismiss()
            })
            .setNeutralButton(R.string.otp_increment_counter, { dialog, _ ->
                counter++
                dialog.dismiss()

                counterOrPeriodEditText.setText(counter.toString())
                updateOtpAuthTextView()
            })
        builder.create().show()
    }

    private fun startOtpProgressAnimation(isFirst: Boolean) {
        val periodInMillis = period * 1000
        val elapsedMillisOfPeriod = System.currentTimeMillis() % periodInMillis
        val progressedMillisOfPeriod = (elapsedMillisOfPeriod / periodInMillis.toFloat()) * 1000
        val anim = ProgressCircleAnimation(
            totpProgressCircle,
            progressedMillisOfPeriod,
            1000.toFloat()
        )
        if (isFirst) {
            anim.duration = periodInMillis.toLong() - elapsedMillisOfPeriod
        }
        else {
            anim.duration = periodInMillis.toLong()
        }
        totpProgressCircle.startAnimation(anim)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerRunner?.let { timer.removeCallbacks(it) }
    }

    private fun updateOtpAuthTextView() {
        otpToSave = createOTPConfigFromCurrentState()
        if (otpToSave == null) {
            hideOtpResults()
        }
        otpToSave?.let {
            otpAuthTextView.text = it.toUri().toString()
            updateCounterOrPeriodTextView(includeValues = false)

            updateCurrentOtpValue()
        }

    }

    private fun hideOtpResults() {
        otpImage.visibility = View.INVISIBLE
        totpProgressCircle.visibility = View.INVISIBLE
        hotpAdjustCounter.visibility = View.INVISIBLE
        otpAuthTextView.text = ""
        otpValueTextView.text = ""
    }

    private fun updateCurrentOtpValue(): Boolean {
        otpToSave?.let {
            val otp = OtpService.generateOTP(it, Date())
            if (otp == null) {
                hideOtpResults()
                return false
            }
            val hasChanged = otpValueTextView.text.toString() != otp.toString()
            otpValueTextView.text = formatOtp(otp.toString())
            if (otpMode == OTPMode.TOTP) {
                otpImage.visibility = View.VISIBLE
                totpProgressCircle.visibility = View.VISIBLE
                hotpAdjustCounter.visibility = View.GONE
            } else if (otpMode == OTPMode.HOTP) {
                otpImage.visibility = View.VISIBLE
                totpProgressCircle.visibility = View.GONE
                hotpAdjustCounter.visibility = View.VISIBLE
            }

            otp.clear()
            return hasChanged
        }
        return false
    }

    private fun formatOtp(otpString: String, masked: Boolean = false, formatted: Boolean = true): String {
        if (masked) {
            return "*".repeat(otpString.length)
        }
        if (!formatted) {
            return otpString
        }
        if (otpString.length == 6) {
            return otpString.substring(0, 3) + " " + otpString.substring(3)
        }
        else if (otpString.length == 7) {
            return otpString.substring(0, 2) + " " + otpString.substring(2, 5) + " " + otpString.substring(5)
        }
        else if (otpString.length == 8) {
            return otpString.substring(0, 4) + " " + otpString.substring(4)
        }
        else if (otpString.length == 9) {
            return otpString.substring(0, 3) + " " + otpString.substring(3, 6) + " " + otpString.substring(6)
        }
        else {
            return otpString
        }
    }

    private fun createOTPConfigFromCurrentState(): OTPConfig? {
        val cDigits = digitsEditText.text.toString().toIntOrNull()
        val cCounterOrPeriod = counterOrPeriodEditText.text.toString().toIntOrNull()
        if (cDigits != null && cCounterOrPeriod != null) {
            secret?.let {
                return OTPConfig(
                    otpMode,
                    if (account.isNullOrBlank()) "" else account!!,
                    issuer ?: "",
                    it,
                    otpAlgorithm,
                    cDigits,
                    cCounterOrPeriod,
                )
            }
        }

        return null
    }

    private fun updateCounterOrPeriodTextView(includeValues: Boolean = true) {
        if (otpMode == OTPMode.HOTP) {
            counterOrPeriodTextView.setText(getString(R.string.otp_current_counter))
            if (includeValues) {
                period = counterOrPeriodEditText.text.toString().toIntOrNull() ?: DEFAULT_OTP_PERIOD
                counterOrPeriodEditText.setText(counter.toString())
            }
        }
        else if (otpMode == OTPMode.TOTP) {
            counterOrPeriodTextView.setText(getString(R.string.otp_period_in_seconds))
            if (includeValues) {
                counter =
                    counterOrPeriodEditText.text.toString().toIntOrNull() ?: DEFAULT_OTP_COUNTER
                counterOrPeriodEditText.setText(period.toString())
            }

        }

    }

    private fun loadOTPFromIntent() {

        val currentCredential = EncCredential.fromIntent(intent)

        masterSecretKey?.let { key ->

            issuer = SecretService.decryptCommonString(key, currentCredential.name)
            account = SecretService.decryptCommonString(key, currentCredential.user)

            currentCredential.otpData?.let { otpData ->
                val otpUriString = SecretService.decryptCommonString(key, otpData.encOtpAuthUri)
                val otpConfig = createOtpConfigFromUri(otpUriString)
                if (otpConfig != null) {
                    updateUIFromOTPConfig(otpConfig)
                }
                else {
                    toastText(this, R.string.otp_cannot_load_config)
                }
            }

        }

    }

    private fun updateUIFromOTPConfig(
        otpConfig: OTPConfig
    ) {

        otpMode = otpConfig.mode
        otpAlgorithm = otpConfig.algorithm
        secret = otpConfig.secret
        if (otpMode == OTPMode.HOTP) {
            counter = otpConfig.periodOrCounter

        } else if (otpMode == OTPMode.TOTP) {
            period = otpConfig.periodOrCounter
        }

        otpModeSelection.setSelection(otpMode.ordinal)
        otpAlgorithmSelection.setSelection(otpAlgorithm.ordinal)

        sharedSecretEditText.setText(otpConfig.secretAsBase32())
        counterOrPeriodEditText.setText(otpConfig.periodOrCounter.toString())
        digitsEditText.setText(otpConfig.digits.toString())

        updateOtpAuthTextView()
    }

    private fun createOtpConfigFromUri(
        otpUriString: String
    ): OTPConfig? {
        try {
            val otpUri = Uri.parse(otpUriString)
            val otpConfig = OTPConfig.fromUri(otpUri)
            return otpConfig
        } catch (e: Exception) {
            DebugInfo.logException("OTP", "Cannot parse URI $otpUriString", e)
            return null
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Session.isDenied()) {
            return false
        }

        menuInflater.inflate(R.menu.menu_configure_otp, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return false
        }

        if (id == R.id.menu_copy_otpauth) {
            otpToSave = createOTPConfigFromCurrentState()
            if (otpToSave != null) {
                ClipboardUtil.copy("OTP-Config", otpToSave!!.toUri().toString(), this, isSensible = true)
                toastText(this, R.string.copied_to_clipboard)
            }
            else {
                toastText(this, R.string.otp_cannot_export)
            }

            return true
        }

        if (id == R.id.menu_export_otp) {

            val tempKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, this)
            otpToSave = createOTPConfigFromCurrentState()

            if (otpToSave == null) {
                toastText(this, R.string.otp_cannot_export)
                return false
            }


            val encHead =
                SecretService.encryptCommonString(
                    tempKey,
                    getString(R.string.otp_config_headline)
                )
            val encSub =
                SecretService.encryptCommonString(tempKey, getString(R.string.otp_exported_otp_desc))
            val encQrcHeader = SecretService.encryptCommonString(
                tempKey,
                otpToSave!!.getLabel())
            val encQrc = SecretService.encryptCommonString(tempKey, otpToSave!!.toUri().toString())

            val intent = Intent(this, QrCodeActivity::class.java)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, encHead)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, encQrcHeader)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, encQrc)
            intent.putExtra(QrCodeActivity.EXTRA_COLOR, Color.RED)

            // will be bypassed to NfcActivity
            intent.putExtra(NfcActivity.EXTRA_WITH_APP_RECORD, true)

            startActivity(intent)

            return true
        }

        if (id == R.id.menu_delete_otp) {

            otpToSave = createOTPConfigFromCurrentState()
            if (otpToSave == null) {
                toastText(this, getString(R.string.nothing_to_delete))
                return false
            }

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.otp_remove_config_title, otpToSave?.getLabel()))
                .setMessage(R.string.otp_remove_config_message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                    dialog.dismiss()
                    masterSecretKey?.let { key ->

                        val data = Intent()
                        data.putEncryptedExtra(EXTRA_CREDENTIAL_OTP_DATA, Encrypted.empty())

                        setResult(Activity.RESULT_OK, data)
                        finish()
                    }
                }
                .setNegativeButton(android.R.string.no, null)
                .show()

            return true
        }


        return super.onOptionsItemSelected(item)
    }


    override fun lock() {
        finish()
    }

    override fun getLayoutId() = R.layout.activity_config_otp

    override fun handleScannedData(scanned: String) {
        val otpConfig = createOtpConfigFromUri(scanned)
        if (otpConfig != null) {
            updateUIFromOTPConfig(otpConfig)
        }
        else {
            toastText(this, getString(R.string.otp_cannot_scann_otp_config))
        }
    }


}


class ProgressCircleAnimation(
    private val progressCircle: CircularProgressIndicator,
    private val from: Float,
    private val to: Float
) :
    Animation() {
    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        super.applyTransformation(interpolatedTime, t)
        val value = from + (to - from) * interpolatedTime
        progressCircle.progress = value.toInt()
    }
}