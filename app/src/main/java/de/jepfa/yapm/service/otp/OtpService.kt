package de.jepfa.yapm.service.otp

import android.net.Uri
import android.util.Log
import de.jepfa.yapm.model.encrypted.OtpData
import de.jepfa.yapm.model.otp.OTPConfig
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import io.ktor.util.toUpperCasePreservingASCIIRules
import java.lang.reflect.UndeclaredThrowableException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import kotlin.math.pow


object OtpService {

    fun generateTOTP(
        otpData: OtpData,
        timestamp: Date,
        key: SecretKeyHolder,
        obfuscationKey: Key?) : Password? {

        val optAuthString = SecretService.decryptCommonString(key, otpData.encOtpAuthUri)
        val OTPConfig = OTPConfig.fromUri(Uri.parse(optAuthString))
        if (OTPConfig == null) {
            Log.w("OTP", "cannot parse TOTP URI $optAuthString")
            return null
        }

        return generateTOTP(OTPConfig, timestamp)
    }

    fun generateHOTP(
        otpConfig: OTPConfig
    ): Password? {
        return generateHOTP(otpConfig, otpConfig.periodOrCounter.toLong())
    }

    fun generateTOTP(
        otpConfig: OTPConfig,
        timestamp: Date
    ): Password? {
        val counter = timestamp.time / (otpConfig.periodOrCounter * 1000)
        return generateHOTP(otpConfig, counter)
    }

    private fun generateHOTP(
        otpConfig: OTPConfig,
        counter: Long
    ): Password? {

        val algorithmIdentifier = getHmacIdentifier(otpConfig.algorithm)
        if (algorithmIdentifier == null) {
            Log.w("OTP", "unknown algorithm: ${otpConfig.algorithm}")
            return null
        }

        val mac = Mac.getInstance(algorithmIdentifier)
        mac.init(SecretKeySpec(otpConfig.secret.toByteArray(), algorithmIdentifier))
        val hmac = mac.doFinal(longToByteArray(counter))

        return Password(
            byteArrayToDigits(hmac, otpConfig.digits)
                .toString()
                .padStart(otpConfig.digits, '0'))
    }

    fun generateTOTP(
        key: String?,
        time: String,
        codeDigits: Int,
        crypto: String?
    ): String? {
      /*  var time = time
        var result: String? = null

        // Using the counter
        // First 8 bytes are for the movingFactor
        // Compliant with base RFC 4226 (HOTP)
        while (time.length < 16) time = "0$time"

        // Get the HEX in a Byte[]
        val msg: ByteArray = hexStr2Bytes(time)
        val k: ByteArray = hexStr2Bytes(key)


        val hash: ByteArray = hmac_sha(crypto, k, msg)

        // put selected bytes into result int
        val offset = hash[hash.size - 1].toInt() and 0xf

        val binary =
            ((hash[offset].toInt() and 0x7f) shl 24) or
                    ((hash[offset + 1].toInt() and 0xff) shl 16) or
                    ((hash[offset + 2].toInt() and 0xff) shl 8) or
                    (hash[offset + 3].toInt() and 0xff)

        val otp: Int = binary % DIGITS_POWER.get(codeDigits)

        result = otp.toString()
        while (result!!.length < codeDigits) {
            result = "0$result"
        }
        return result*/return null
    }

    private fun hmac_sha(
        crypto: String, keyBytes: ByteArray,
        text: ByteArray
    ): ByteArray {
        try {
            val hmac = Mac.getInstance(crypto)
            val macKey =
                SecretKeySpec(keyBytes, "RAW")
            hmac.init(macKey)
            return hmac.doFinal(text)
        } catch (gse: GeneralSecurityException) {
            throw UndeclaredThrowableException(gse)
        }
    }


    /**
     * This method converts a HEX string to Byte[]
     *
     * @param hex: the HEX string
     *
     * @return: a byte array
     */
    private fun hexStr2Bytes(hex: String): ByteArray {
        // Adding one byte to get the right conversion
        // Values starting with "0" can be converted
        val bArray = BigInteger("10$hex", 16).toByteArray()

        // Copy all the REAL bytes, not the "first"
        val ret = ByteArray(bArray.size - 1)
        for (i in ret.indices) ret[i] = bArray[i + 1]
        return ret
    }

    private
    val DIGITS_POWER // 0 1  2   3    4     5      6       7        8
            : IntArray = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)

    private fun getHmacIdentifier(algorithm: String): String? {
        return when (algorithm.replace(Regex("[^A-Za-z0-9]"), "").toUpperCasePreservingASCIIRules()) {
            "SHA1" -> "HmacSHA1"
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else -> null
        }
    }

    private fun longToByteArray(long: Long): ByteArray {

        return ByteBuffer.allocate(Long.SIZE_BYTES).putLong(long).array()
       /* var movingFactor = long
        val bytes = ByteArray(8)
        for (i in bytes.indices.reversed()) {
            bytes[i] = (movingFactor and 0xff).toByte()
            movingFactor = movingFactor shr 8
        }
        return bytes*/
    }

    private fun byteArrayToDigits(bytes: ByteArray, digits: Int): Int {


        val offset = bytes[bytes.size - 1].toInt() and 0xf
     /*   if ((0 <= truncationOffset) &&
            (truncationOffset < (hash.length - 4))
        ) {
            offset = truncationOffset
        }*/

        val binary =
            (((bytes[offset] and 0x7f).toInt() shl 24)
                    or ((bytes[offset + 1].toInt() and 0xff) shl 16)
                    or ((bytes[offset + 2].toInt() and 0xff) shl 8)
                    or (bytes[offset + 3].toInt() and 0xff))
/*
        val binary =
            ((bytes[offset].toInt() and 0x7f) shl 24 or ((bytes[offset + 1].toInt() and 0xff) shl 16
                    ) or ((bytes[offset + 2].toInt() and 0xff) shl 8
                    ) or (bytes[offset + 3].toInt() and 0xff))*/


        return binary % 10.0.pow(digits).toInt()

    }

}