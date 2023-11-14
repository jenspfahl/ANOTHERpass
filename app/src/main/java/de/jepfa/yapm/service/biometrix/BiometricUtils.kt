package de.jepfa.yapm.service.biometrix

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager

object BiometricUtils {
    val isBiometricPromptAvailable: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun isBiometricsAvailable(context: Context): Boolean {
        return isBiometricsSupported(context)
                && hasBiometricsEnrolled(context)
                && isPermissionGranted(context)
    }

    fun hasBiometricsEnrolled(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        val canAuthenticate = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    // supported but not enrolled
    fun isBiometricsSupported(context: Context): Boolean {
        val bm = BiometricManager.from(context)
        val canAuthenticate = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS || canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }

    fun isPermissionGranted(context: Context): Boolean {
        if (isBiometricPromptAvailable) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_BIOMETRIC) ==
                    PackageManager.PERMISSION_GRANTED
        }
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) ==
                PackageManager.PERMISSION_GRANTED
    }

}