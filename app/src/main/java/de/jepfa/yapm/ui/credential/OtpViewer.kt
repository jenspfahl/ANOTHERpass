package de.jepfa.yapm.ui.credential

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.progressindicator.CircularProgressIndicator
import de.jepfa.yapm.R
import de.jepfa.yapm.model.otp.OtpConfig
import de.jepfa.yapm.model.otp.OtpMode
import de.jepfa.yapm.service.otp.OtpService
import de.jepfa.yapm.ui.ProgressCircleAnimation
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.isDarkMode
import de.jepfa.yapm.util.toastText
import java.util.Date

class OtpViewer(
    var otpConfig: OtpConfig?,
    private val activity: SecureActivity,
    private val hotpCounterChanged: () -> Unit,
    private var masked: Boolean = false,
    ) {

    private val timer = Handler(Looper.getMainLooper())
    private var timerRunner: Runnable? = null

    private val otpView: View = activity.findViewById(R.id.otp_view)
    private val otpImage: View = activity.findViewById(R.id.otp_image)
    private val totpProgressCircle: CircularProgressIndicator = activity.findViewById(R.id.totp_progress_circle)
    private val hotpAdjustCounter: ImageView = activity.findViewById(R.id.hotp_adjust_counter)
    private val otpValueTextView: TextView = activity.findViewById(R.id.otp_value)

    private var currentOtpValue: String = ""

    private var hideHotpCounterAdjuster = false

    init {

        if (isDarkMode(activity)) {
            totpProgressCircle.setIndicatorColor(activity.getColor(R.color.Gray2))
        }

        hotpAdjustCounter.setOnClickListener {
            if (otpConfig?.mode == OtpMode.HOTP) {
                adjustHOTPCounter()
            }
        }

        val unmasking: (View) -> Unit = {
            masked = false
            refreshVisibility()
        }
        otpValueTextView.setOnClickListener(unmasking)
        otpImage.setOnClickListener(unmasking)
        otpView.setOnClickListener(unmasking)

        val copying: (View) -> Boolean = {
            otpConfig?.let {
                if (currentOtpValue.isNotBlank()) {
                    ClipboardUtil.copy(it.getLabel(), currentOtpValue, activity, isSensible = true)
                    toastText(activity, R.string.copied_to_clipboard)
                }
            }

            true
        }
        otpValueTextView.setOnLongClickListener(copying)
        otpImage.setOnLongClickListener(copying)
        otpView.setOnLongClickListener(copying)

        if (otpConfig == null) {
            hideOtpView()
        }
    }

    fun start() {
        timerRunner = Runnable {
            val hasChanged = updateCurrentOtpValue()
            timerRunner?.let {
                timer.postDelayed(it, 1000L)
                if (otpConfig?.mode == OtpMode.TOTP && hasChanged) {
                    startTotpProgressAnimation()
                }
            }
        }
        val elapsedTimeOfSecond = System.currentTimeMillis() % 1000
        val firstDelay = 1000 - elapsedTimeOfSecond
        timer.postDelayed(timerRunner!!, firstDelay)

        if (otpConfig?.mode == OtpMode.TOTP) {
            startTotpProgressAnimation()
        }
    }

    private fun updateCurrentOtpValue(): Boolean {
        otpConfig?.let { otp ->
            val totp = OtpService.generateOTP(otp, Date())
            if (totp == null) {
                hideOtpView()
                return false
            }
            otpView.visibility = View.VISIBLE
            val newTotpValue = totp.toString()
            otpValueTextView.text = formatOtp(newTotpValue, masked = masked)
            val hasChanged = newTotpValue != currentOtpValue
            currentOtpValue = newTotpValue

            return hasChanged
        }

        return false

    }


    fun stop() {
        timerRunner?.let { timer.removeCallbacks(it) }
    }

    private fun hideOtpView() {
        otpView.visibility = View.INVISIBLE
    }

    private fun adjustHOTPCounter() {
        val builder = AlertDialog.Builder(activity)
        val view: View = activity.layoutInflater.inflate(R.layout.number_picker_dialog, null)
        builder.setView(view)

        builder.setTitle(R.string.otp_update_hotp_counter_title)
        builder.setMessage(activity.getString(R.string.otp_update_hotp_counter_message, otpConfig?.counter.toString()))
        val picker = view.findViewById<View>(R.id.number_picker) as NumberPicker


        picker.minValue = 1
        picker.maxValue = 99_999_999
        picker.value = otpConfig?.counter ?: 0
        builder
            .setPositiveButton(android.R.string.ok, { dialog, _ ->
                dialog.dismiss()
                otpConfig?.counter = picker.value
                hotpCounterChanged()
            })
            .setNegativeButton(android.R.string.cancel, { dialog, _ ->
                dialog.dismiss()
            })
            .setNeutralButton(R.string.otp_increment_counter, { dialog, _ ->
                dialog.dismiss()
                otpConfig?.incCounter()
                hotpCounterChanged()
            })
        builder.create().show()
    }

    private fun startTotpProgressAnimation() {
        if (otpConfig?.mode == OtpMode.HOTP) {
            return
        }
        val periodInMillis = (otpConfig?.period ?: 0) * 1000
        val elapsedMillisOfPeriod = System.currentTimeMillis() % periodInMillis
        val progressedMillisOfPeriod = (elapsedMillisOfPeriod / periodInMillis.toFloat()) * 1000
        val anim = ProgressCircleAnimation(
            totpProgressCircle,
            progressedMillisOfPeriod,
            1000.toFloat()
        )

        anim.duration = periodInMillis.toLong() - elapsedMillisOfPeriod

        totpProgressCircle.startAnimation(anim)
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

    fun refreshVisibility() {
        if (otpConfig == null) {
            hideOtpView()
            return
        }
        if (otpConfig?.mode == OtpMode.HOTP) {
            totpProgressCircle.visibility = View.GONE
            if (masked) {
                hotpAdjustCounter.visibility = View.INVISIBLE
            }
            else if (!hideHotpCounterAdjuster) {
                hotpAdjustCounter.visibility = View.VISIBLE
            }
        }
        else if (otpConfig?.mode == OtpMode.TOTP) {
            if (masked) {
                totpProgressCircle.visibility = View.INVISIBLE
            }
            else {
                totpProgressCircle.visibility = View.VISIBLE
            }
            hotpAdjustCounter.visibility = View.GONE
        }
        updateCurrentOtpValue()
    }

    fun hideHotpCounterAdjuster() {
        hotpAdjustCounter.visibility = View.INVISIBLE
        this.hideHotpCounterAdjuster = true
    }


}