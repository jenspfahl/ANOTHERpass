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

enum class OTPMode(val uiLabel: Int, val uriName: String) {

    HOTP(R.string.hotp_name, "hotp"),
    TOTP(R.string.totp_name, "totp");

    companion object {
        fun getValueFrom(uriName: String): OTPMode {
            return entries.first { it.uriName == uriName }
        }
    }

}

enum class OTPAlgorithm(val uiName: String, val uriName: String, val algoHmacName: String) {

    SHA1("SHA-1 (RFC-4226)", "SHA1", "HmacSHA1"),
    SHA256("SHA-256", "SHA256", "HmacSHA256"),
    SHA512("SHA-512", "SHA512", "HmacSHA512");

    companion object {
        fun getValueFrom(uriName: String): OTPAlgorithm {
            return entries.first { it.uriName == uriName }
        }
    }

}

data class OTPConfig(
    val mode: OTPMode,
    val account: String,
    val issuer: String,
    val secret: Key,
    val algorithm: OTPAlgorithm,
    val digits: Int,
    var periodOrCounter: Int) {

    private fun getLabel(): String {
        if (account.isBlank()) {
            return issuer.encodeURLPath()
        }
        return "${issuer.encodeURLPath()}:${account.encodeURLPath()}"
    }

    fun toUri(): Uri {
        val periodOrCounterParam = if (mode == OTPMode.TOTP)
            "$PARAM_PERIOD=$periodOrCounter"
        else if (mode == OTPMode.HOTP)
            "$PARAM_COUNTER=$periodOrCounter"
        else
            throw IllegalArgumentException("illegal mode: $mode")
        val s = "$OTP_SCHEME://${mode.uriName}/${getLabel()}?$PARAM_SECRET=${secretAsBase32()}&$PARAM_ISSUER=${issuer.encodeURLPath()}&$PARAM_ALGORITHM=${algorithm.uriName}&$PARAM_DIGITS=$digits&$periodOrCounterParam"
        return Uri.parse(s)
    }

    fun incCounter(): OTPConfig {
        if (mode == OTPMode.HOTP) {
            periodOrCounter++
        }

        return this
    }

    fun secretAsBase32(): String = Base32().encodeToString(secret.toByteArray())

    companion object {
        val DEFAULT_OTP_MODE = OTPMode.TOTP
        val DEFAULT_OTP_ALGORITHM = OTPAlgorithm.SHA1
        val DEFAULT_OTP_COUNTER = 1
        val DEFAULT_OTP_PERIOD = 30
        val DEFAULT_OTP_DIGITS = 6

        fun fromUri(uri: Uri): OTPConfig? {

            if (uri.scheme != OTP_SCHEME) {
                Log.w("OTP", "Illegal scheme: ${uri.scheme}")
                return null
            }

            val modeAsString = uri.host
            if (modeAsString != OTPMode.HOTP.uriName && modeAsString != OTPMode.TOTP.uriName) {
                Log.w("OTP", "Wrong method: $uri")

                return null
            }
            val mode = OTPMode.getValueFrom(modeAsString)

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


            val secretAsBase64 = uri.getQueryParameter(PARAM_SECRET) ?: return null
            val issuerFromParams = uri.getQueryParameter(PARAM_ISSUER)

            val algorithmAsString = uri.getQueryParameter(PARAM_ALGORITHM)
            val algorithm = if (algorithmAsString != null)
                OTPAlgorithm.getValueFrom(algorithmAsString)
            else
                DEFAULT_OTP_ALGORITHM

            val digits = uri.getQueryParameter(PARAM_DIGITS)?.toIntOrNull() ?: 6
            val periodInSec = uri.getQueryParameter(PARAM_PERIOD)?.toIntOrNull() ?: 30
            val counter = uri.getQueryParameter(PARAM_COUNTER)?.toIntOrNull() ?: 1

            if (issuerFromParams != issuerFromLabel) {
                // warn
            }

            return OTPConfig(
                mode,
                accountFromLabel ?: label,
                issuerFromLabel,
                Key(Base32().decode(secretAsBase64)),
                algorithm,
                digits,
                if (mode == OTPMode.HOTP) counter else periodInSec
            )
        }
    }
}
