package de.jepfa.yapm.model.otp

import android.net.Uri
import android.util.Log
import de.jepfa.yapm.model.secret.Key
import io.ktor.http.encodeURLPath
import io.ktor.util.decodeBase64Bytes

private const val OTP_SCHEME = "otpauth"

private const val PATH_HOTP = "hotp"
private const val PATH_TOTP = "totp"

private const val PARAM_SECRET = "secret"
private const val PARAM_ISSUER = "issuer"
private const val PARAM_ALGORITHM = "algorithm"
private const val PARAM_DIGITS = "digits"
private const val PARAM_PERIOD = "period"
private const val PARAM_COUNTER = "counter"

data class TOTPConfig(
    val account: String,
    val issuer: String,
    val secret: Key,
    val algorithm: String,
    val digits: Int,
    val periodOrCounter: Int) {

    private fun getLabel() = "${issuer.encodeURLPath()}:${account.encodeURLPath()}"

    fun toUri(): Uri {
        val s = "$OTP_SCHEME://$PATH_TOTP/${getLabel()}?$PARAM_SECRET=${secret.toBase64String()}&$PARAM_ISSUER=${issuer.encodeURLPath()}&$PARAM_ALGORITHM=$algorithm&$PARAM_DIGITS=$digits&$PARAM_PERIOD=$periodOrCounter"
        return Uri.parse(s)
    }

    companion object {
        fun fromUri(uri: Uri): TOTPConfig? {

            if (uri.scheme != OTP_SCHEME) {
                Log.w("OTP", "Illegal scheme: ${uri.scheme}")
                return null
            }

            val method = uri.host
            if (method != PATH_HOTP && method != PATH_TOTP) {
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
            val counter = uri.getQueryParameter(PARAM_COUNTER)?.toIntOrNull()


            if (issuerFromParams != issuerFromLabel) {
                // warn
            }

            return TOTPConfig(
                accountFromLabel ?: label,
                issuerFromLabel,
                Key(secretAsBase64.decodeBase64Bytes()),
                algorithm,
                digits,
                counter ?: periodInSec
            )
        }
    }
}
