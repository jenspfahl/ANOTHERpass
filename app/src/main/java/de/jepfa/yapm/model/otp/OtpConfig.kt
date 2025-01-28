package de.jepfa.yapm.model.otp

import android.net.Uri
import android.util.Log
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Key
import io.ktor.http.encodeURLPath
import org.apache.commons.codec.binary.Base32

private const val OTP_SCHEME = "otpauth"


private const val PARAM_SECRET = "secret"
private const val PARAM_ISSUER = "issuer"
private const val PARAM_ALGORITHM = "algorithm"
private const val PARAM_DIGITS = "digits"
private const val PARAM_PERIOD = "period"
private const val PARAM_COUNTER = "counter"

enum class OtpMode(val uiLabel: Int, val uriName: String) {

    HOTP(R.string.hotp_name, "hotp"),
    TOTP(R.string.totp_name, "totp");

    companion object {
        fun getValueFrom(uriName: String): OtpMode {
            return entries.first { it.uriName == uriName }
        }
    }

}

enum class OtpAlgorithm(val uiName: String, val uriName: String, val algoHmacName: String) {

    SHA1("SHA-1", "SHA1", "HmacSHA1"),
    SHA256("SHA-256", "SHA256", "HmacSHA256"),
    SHA512("SHA-512", "SHA512", "HmacSHA512");

    companion object {
        fun getValueFrom(uriName: String): OtpAlgorithm {
            return entries.first { it.uriName == uriName }
        }
    }

}

data class OtpConfig(
    var mode: OtpMode,
    var account: String,
    var issuer: String,
    var secret: Key,
    var algorithm: OtpAlgorithm,
    var digits: Int,
    var period: Int,
    var counter: Int,) {

    fun getLabel(): String {
        if (account.isBlank()) {
            return issuer
        }
        return "$issuer:$account"
    }

    fun toUri(): Uri {
        val periodOrCounterParam = if (mode == OtpMode.TOTP)
            "$PARAM_PERIOD=$period"
        else if (mode == OtpMode.HOTP)
            "$PARAM_COUNTER=$counter"
        else
            throw IllegalArgumentException("illegal mode: $mode")
        val s = "$OTP_SCHEME://${mode.uriName}/${getLabel().encodeURLPath()}?$PARAM_SECRET=${secretAsBase32()}&$PARAM_ISSUER=${issuer.encodeURLPath()}&$PARAM_ALGORITHM=${algorithm.uriName}&$PARAM_DIGITS=$digits&$periodOrCounterParam"
        return Uri.parse(s)
    }

    override fun toString(): String {
        return toUri().toString()
    }

    fun incCounter(): OtpConfig {
        if (mode == OtpMode.HOTP) {
            counter++
        }

        return this
    }

    fun secretAsBase32(): String = Base32().encodeToString(secret.toByteArray())

    fun isValid(): Boolean {
        if (secret.isEmpty()) {
            return false
        }
        if (issuer.isBlank()) {
            return false
        }
        if (mode == OtpMode.TOTP && period <= 0) {
            return false
        }
        if (mode == OtpMode.HOTP && counter < 0) {
            return false
        }
        if (digits <= 0) {
            return false
        }

        return true
    }


    companion object {
        val DEFAULT_OTP_MODE = OtpMode.TOTP
        val DEFAULT_OTP_ALGORITHM = OtpAlgorithm.SHA1
        val DEFAULT_OTP_COUNTER = 1
        val DEFAULT_OTP_PERIOD = 30
        val DEFAULT_OTP_DIGITS = 6

        fun fromUri(uri: Uri): OtpConfig? {

            if (uri.scheme != OTP_SCHEME) {
                Log.w("OTP", "Illegal scheme: ${uri.scheme}")
                return null
            }

            val modeAsString = uri.host
            if (modeAsString != OtpMode.HOTP.uriName && modeAsString != OtpMode.TOTP.uriName) {
                Log.w("OTP", "Wrong method: $uri")

                return null
            }
            val mode = OtpMode.getValueFrom(modeAsString)

            val label = uri.path
            if (label == null) {
                Log.w("OTP", "Missing label: $uri")
                return null
            }



            val labelParts = label.split(":")
            if (labelParts.isEmpty() || labelParts.size > 2) {
                Log.w("OTP", "No label")

                return null
            }

            val issuerFromLabel = labelParts[0].replace("/", "")
            val accountFromLabel = if (labelParts.size > 1) labelParts[1] else null


            val secretAsBase32 = uri.getQueryParameter(PARAM_SECRET) ?: return null
            val issuerFromParams = uri.getQueryParameter(PARAM_ISSUER)

            val algorithmAsString = uri.getQueryParameter(PARAM_ALGORITHM)
            val algorithm = if (algorithmAsString != null)
                OtpAlgorithm.getValueFrom(algorithmAsString)
            else
                DEFAULT_OTP_ALGORITHM

            val digits = uri.getQueryParameter(PARAM_DIGITS)?.toIntOrNull() ?: DEFAULT_OTP_DIGITS
            val periodInSec = uri.getQueryParameter(PARAM_PERIOD)?.toIntOrNull() ?: DEFAULT_OTP_PERIOD
            val counter = uri.getQueryParameter(PARAM_COUNTER)?.toIntOrNull() ?: DEFAULT_OTP_COUNTER

            if (issuerFromParams != issuerFromLabel) {
                // warn
            }

            return OtpConfig(
                mode,
                accountFromLabel ?: label,
                issuerFromLabel,
                stringToBase32Key(secretAsBase32),
                algorithm,
                digits,
                periodInSec,
                counter)
        }

        fun createFromPacked(packed: String?, name: String, user: String): OtpConfig? {
            if (packed.isNullOrBlank()) {
                return null
            }
            val splitted = packed.split("_")
            if (splitted.size != 5) {
                return null
            }

            val modeId = splitted[0].toIntOrNull() ?: return null
            val algorithmId = splitted[1].toIntOrNull() ?: return null
            val mode = OtpMode.entries.getOrNull(modeId) ?: return null
            val algorithm = OtpAlgorithm.entries.getOrNull(algorithmId) ?: return null
            val secret = stringToBase32Key(splitted[2])
            val digits = splitted[3].toIntOrNull() ?: return null
            val periodOrCounter = splitted[4].toIntOrNull() ?: return null
            val otpConfig = OtpConfig(
                mode,
                name,
                user,
                secret,
                algorithm,
                digits,
                if (mode == OtpMode.TOTP) periodOrCounter else OtpConfig.DEFAULT_OTP_PERIOD,
                if (mode == OtpMode.HOTP) periodOrCounter else OtpConfig.DEFAULT_OTP_COUNTER,
            )

            return otpConfig
        }

        fun packOtpAuthUri(otpAuthUri: String): String? {
            val otpConfig = fromUri(Uri.parse(otpAuthUri)) ?: return null
            val counterOrPeriod = if (otpConfig.mode == OtpMode.HOTP) otpConfig.counter else otpConfig.period
            return "${otpConfig.mode.ordinal}_${otpConfig.algorithm.ordinal}_${otpConfig.secretAsBase32()}_${otpConfig.digits}_${counterOrPeriod}"
        }

        fun stringToBase32Key(base32String: String) = Key(Base32().decode(base32String))
    }
}
