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
import de.jepfa.yapm.model.otp.OTPConfig
import de.jepfa.yapm.model.otp.OTPMode
import de.jepfa.yapm.service.otp.OtpService
import de.jepfa.yapm.ui.ProgressCircleAnimation
import de.jepfa.yapm.ui.SecureActivity
import java.util.Date

class OtpViewer(
    var otpConfig: OTPConfig?,
    private val activity: SecureActivity,
    private val hotpCounterChanged: () -> Unit,
    ) {

    private val timer = Handler(Looper.getMainLooper())
    private var timerRunner: Runnable? = null

    private val otpView: View = activity.findViewById(R.id.otp_view)
    private val totpProgressCircle: CircularProgressIndicator = activity.findViewById(R.id.totp_progress_circle)
    private val hotpAdjustCounter: ImageView = activity.findViewById(R.id.hotp_adjust_counter)
    private val otpValueTextView: TextView = activity.findViewById(R.id.otp_value)

    private var currentTotpValue: String = ""

    init {

        hotpAdjustCounter.setOnClickListener {
            if (otpConfig?.mode == OTPMode.HOTP) {
                adjustHOTPCounter()
            }
        }

        if (otpConfig == null) {
            hideOtpView()
        }
    }

    fun start() {
        timerRunner = Runnable {
            val hasChanged = updateCurrentTotpValue()
            timerRunner?.let {
                timer.postDelayed(it, 1000L)
                if (otpConfig?.mode == OTPMode.TOTP && hasChanged) {
                    startTotpProgressAnimation()
                }
            }
        }
        val elapsedTimeOfSecond = System.currentTimeMillis() % 1000
        val firstDelay = 1000 - elapsedTimeOfSecond
        timer.postDelayed(timerRunner!!, firstDelay)

        if (otpConfig?.mode == OTPMode.TOTP) {
            startTotpProgressAnimation()
        }
    }

    private fun updateCurrentTotpValue(): Boolean {
        otpConfig?.let { otp ->
            val totp = OtpService.generateOTP(otp, Date())
            if (totp == null) {
                hideOtpView()
                return false
            }
            val newTotpValue = totp.toString()
            otpValueTextView.text = formatOtp(newTotpValue)
            val hasChanged = newTotpValue != currentTotpValue
            currentTotpValue = newTotpValue

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
        builder.setMessage(activity.getString(R.string.otp_update_hotp_counter_message, otpConfig?.periodOrCounter.toString()))
        val picker = view.findViewById<View>(R.id.number_picker) as NumberPicker


        picker.minValue = 1
        picker.maxValue = 99_999_999
        picker.value = otpConfig?.periodOrCounter ?: 0
        builder
            .setPositiveButton(android.R.string.ok, { dialog, _ ->
                dialog.dismiss()
                otpConfig?.periodOrCounter = picker.value
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
        if (otpConfig?.mode == OTPMode.HOTP) {
            return
        }
        val periodInMillis = (otpConfig?.periodOrCounter ?: 0) * 1000
        val elapsedMillisOfPeriod = System.currentTimeMillis() % periodInMillis
        val progressedMillisOfPeriod = (elapsedMillisOfPeriod / periodInMillis.toFloat()) * 1000
        val anim = ProgressCircleAnimation(
            totpProgressCircle,
            progressedMillisOfPeriod,
            1000.toFloat()
        )

        anim.duration = periodInMillis.toLong()

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

    fun refreshControllerVisibility() {
        if (otpConfig?.mode == OTPMode.HOTP) {
            totpProgressCircle.visibility = View.GONE
            hotpAdjustCounter.visibility = View.VISIBLE
        }
        else if (otpConfig?.mode == OTPMode.TOTP) {
            totpProgressCircle.visibility = View.VISIBLE
            hotpAdjustCounter.visibility = View.GONE
        }
    }


}