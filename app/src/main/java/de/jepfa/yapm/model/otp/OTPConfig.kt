package de.jepfa.yapm.model.otp

import android.net.Uri
import android.util.Log
import de.jepfa.yapm.model.secret.Key
import io.ktor.http.encodeURLPath
import org.apache.commons.codec.binary.Base32

private const val OTP_SCHEME = "otpauth"

private const val MODE_HOTP = "hotp"
private const val MODE_TOTP = "totp"

private const val PARAM_SECRET = "secret"
private const val PARAM_ISSUER = "issuer"
private const val PARAM_ALGORITHM = "algorithm"
private const val PARAM_DIGITS = "digits"
private const val PARAM_PERIOD = "period"
private const val PARAM_COUNTER = "counter"

data class OTPConfig(
    val mode: String,
    val account: String,
    val issuer: String,
    val secret: Key,
    val algorithm: String,
    val digits: Int,
    var periodOrCounter: Int) {

    private fun getLabel() = "${issuer.encodeURLPath()}:${account.encodeURLPath()}"

    fun toUri(): Uri {
        val periodOrCounterParam = if (mode == MODE_TOTP)
            "$PARAM_PERIOD=$periodOrCounter"
        else if (mode == MODE_HOTP)
            "$PARAM_COUNTER=$periodOrCounter"
        else
            throw IllegalArgumentException("illegal mode: $mode")
        val s = "$OTP_SCHEME://$mode/${getLabel()}?$PARAM_SECRET=${Base32().encodeToString(secret.toByteArray())}&$PARAM_ISSUER=${issuer.encodeURLPath()}&$PARAM_ALGORITHM=$algorithm&$PARAM_DIGITS=$digits&$periodOrCounterParam"
        return Uri.parse(s)
    }

    fun incCounter(): OTPConfig {
        if (mode == MODE_HOTP) {
            periodOrCounter++
        }

        return this
    }

    companion object {
        fun fromUri(uri: Uri): OTPConfig? {

            if (uri.scheme != OTP_SCHEME) {
                Log.w("OTP", "Illegal scheme: ${uri.scheme}")
                return null
            }

            val mode = uri.host
            if (mode != MODE_HOTP && mode != MODE_TOTP) {
                Log.w("OTP", "Wrong method: $uri")

                return null
            }

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
            val algorithm = uri.getQueryParameter(PARAM_ALGORITHM) ?: "SHA-256"
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
                if (mode == MODE_HOTP) counter else periodInSec
            )
        }
    }
}
