package de.jepfa.yapm.model.encrypted

import android.os.Build
import de.jepfa.yapm.R

val DEFAULT_CIPHER_ALGORITHM = CipherAlgorithm.AES_128

enum class CipherAlgorithm(
    val keyLength: Int,
    val cipherName: String,
    val secretKeyAlgorithm: String,
    val gcmSupport: Boolean,
    val integratedIvSupport: Boolean,
    val uiLabel: Int,
    val description: Int,
    val supportedSdkVersion: Int) {
    AES_128(128, "AES/GCM/NoPadding", "PBKDF2WithHmacSHA1", true, true, R.string.CIPHER_AES_128, R.string.CIPHER_AES_128_desc, Build.VERSION_CODES.BASE),
    AES_256(256, "AES/GCM/NoPadding", "PBKDF2WithHmacSHA256", true, true, R.string.CIPHER_AES_256, R.string.CIPHER_AES_256_desc, Build.VERSION_CODES.O),
    BLOWFISH_128(128, "BLOWFISH/CBC/PKCS5Padding", "PBKDF2WithHmacSHA1", false, false, R.string.CIPHER_BLOWFISH_128, R.string.CIPHER_BLOWFISH_128_desc, Build.VERSION_CODES.GINGERBREAD_MR1),
    BLOWFISH_256(256, "BLOWFISH/CBC/PKCS5Padding", "PBKDF2WithHmacSHA1", false, false, R.string.CIPHER_BLOWFISH_256, R.string.CIPHER_BLOWFISH_256_desc, Build.VERSION_CODES.GINGERBREAD_MR1),
    CHACHACHA20(256, "ChaCha20/Poly1305/NoPadding", "PBKDF2WithHmacSHA256", false, true, R.string.CIPHER_CHACHACHA20_256, R.string.CIPHER_CHACHACHA20_256_desc, Build.VERSION_CODES.P),
    ;
    companion object {
        fun supportedValues(): List<CipherAlgorithm> {
            return CipherAlgorithm.values()
                .filter { Build.VERSION.SDK_INT >= it.supportedSdkVersion }
        }
    }
}