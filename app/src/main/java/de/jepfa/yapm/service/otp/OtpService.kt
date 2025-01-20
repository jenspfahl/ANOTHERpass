package de.jepfa.yapm.service.otp

import android.net.Uri
import android.util.Log
import de.jepfa.yapm.model.encrypted.OtpData
import de.jepfa.yapm.model.otp.OTPConfig
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import java.nio.ByteBuffer
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
        val otpConfig = OTPConfig.fromUri(Uri.parse(optAuthString))
        if (otpConfig == null) {
            Log.w("OTP", "cannot parse TOTP URI $optAuthString")
            return null
        }

        return generateTOTP(otpConfig, timestamp)
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
    ): Password {

        val algorithmIdentifier = otpConfig.algorithm.algoHmacName

        val mac = Mac.getInstance(algorithmIdentifier)
        mac.init(SecretKeySpec(otpConfig.secret.toByteArray(), algorithmIdentifier))
        val hmac = mac.doFinal(longToByteArray(counter))

        return Password(
            byteArrayToDigits(hmac, otpConfig.digits)
                .toString()
                .padStart(otpConfig.digits, '0'))
    }

    private fun longToByteArray(long: Long): ByteArray {
        return ByteBuffer.allocate(Long.SIZE_BYTES).putLong(long).array()
    }

    private fun byteArrayToDigits(bytes: ByteArray, digits: Int): Int {


        val offset = bytes[bytes.size - 1].toInt() and 0xf

        val binary =
            (((bytes[offset] and 0x7f).toInt() shl 24)
                    or ((bytes[offset + 1].toInt() and 0xff) shl 16)
                    or ((bytes[offset + 2].toInt() and 0xff) shl 8)
                    or (bytes[offset + 3].toInt() and 0xff))


        return binary % 10.0.pow(digits).toInt()
    }

}