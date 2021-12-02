package de.jepfa.yapm.service.biometrix

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt

/*
Taken from https://github.com/FSecureLABS/android-keystore-audit/tree/master/keystorecrypto-app
 */
@SuppressLint("MissingPermission")
object BiometricUtils {
    val isBiometricPromptEnabled: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun isBiometricsAvailable(context: Context): Boolean {
        return isHardwareSupported(context)
                && isFingerprintAvailable(context)
                && isPermissionGranted(context)
    }

    /*
     * Condition II: Check if the device has fingerprint sensors.
     * Note: If you marked android.hardware.fingerprint as something that
     * your app requires (android:required="true"), then you don't need
     * to perform this check.
     *
     * */
    fun isHardwareSupported(context: Context): Boolean {
        val fingerprintManager = FingerprintManagerCompat.from(context)
        return fingerprintManager.isHardwareDetected
    }

    /*
     * Condition III: Fingerprint authentication can be matched with a
     * registered fingerprint of the user. So we need to perform this check
     * in order to enable fingerprint authentication
     *
     * */
    fun isFingerprintAvailable(context: Context): Boolean {
        val fingerprintManager = FingerprintManagerCompat.from(context)
        return fingerprintManager.hasEnrolledFingerprints()
    }

    /*
     * Condition IV: Check if the permission has been added to
     * the app. This permission will be granted as soon as the user
     * installs the app on their device.
     *
     * */
    fun isPermissionGranted(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) ==
                PackageManager.PERMISSION_GRANTED
    }

}