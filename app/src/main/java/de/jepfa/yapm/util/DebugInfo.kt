package de.jepfa.yapm.util

import android.content.Context
import android.os.Build
import de.jepfa.yapm.BuildConfig
import de.jepfa.yapm.database.YapmDatabase
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_VERSION
import de.jepfa.yapm.service.biometrix.BiometricUtils
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.Constants.INITIAL_VAULT_VERSION

object DebugInfo {

    private var debug = BuildConfig.DEBUG

    val isDebug: Boolean
        get() = debug

    @Synchronized
    fun toggleDebug() {
        debug = !debug
    }

    fun getVersionName(context: Context): String {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return pInfo.versionName
    }

    fun getVersionCode(context: Context): Int {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return pInfo.versionCode
    }

    fun getVersionCodeForWhatsNew(context: Context): Int {
        return getVersionCode(context) / 1000
    }

    fun getDebugInfo(context: Context): String {
        val sb = StringBuilder()
        sb.append("\n************ APP INFORMATION ***********\n")
        sb.addFormattedLine("Version", getVersionName(context))
        sb.addFormattedLine("Version Code", getVersionCode(context))
        sb.addFormattedLine("Database Version", YapmDatabase.getVersion())
        sb.addFormattedLine("Vault Version", PreferenceService.getAsString(DATA_VAULT_VERSION, context) ?: INITIAL_VAULT_VERSION)
        sb.addFormattedLine("Vault Cipher", SecretService.getCipherAlgorithm(context))
        sb.addFormattedLine("Build Timestamp", BuildConfig.BUILD_TIME.toSimpleDateTimeFormat())
        sb.addFormattedLine("Build Type", BuildConfig.BUILD_TYPE)

        sb.append("\n************ DEVICE INFORMATION ***********\n")
        sb.addFormattedLine("Brand", Build.BRAND)
        sb.addFormattedLine("Manufacturer", Build.MANUFACTURER)
        sb.addFormattedLine("Device", Build.DEVICE)
        sb.addFormattedLine("Model", Build.MODEL)
        sb.addFormattedLine("Product", Build.PRODUCT)
        sb.addFormattedLine("Hardware", Build.HARDWARE)
        sb.addFormattedLine("OS Build Id", Build.ID)
        sb.addFormattedLine("NFC available", NfcService.isNfcAvailable(context))
        sb.addFormattedLine("NFC enabled", NfcService.isNfcEnabled(context))
        sb.addFormattedLine("Has StrongBox support", SecretService.hasStrongBoxSupport(context))
        sb.addFormattedLine("Has biometrics support", BiometricUtils.isHardwareSupported(context))
        sb.addFormattedLine("Is fingerprint enroled", BiometricUtils.isFingerprintAvailable(context))

        sb.append("\n************ PERMISSIONS ************\n")
        sb.addFormattedLine("Read Storage granted", PermissionChecker.hasReadStoragePermissions(context))
        sb.addFormattedLine("Read/write storage granted", PermissionChecker.hasRWStoragePermissions(context))
        sb.addFormattedLine("Overlay dialog granted", PermissionChecker.hasOverlayPermission(context))
        sb.addFormattedLine("Camera granted", PermissionChecker.hasCameraPermission(context))
        sb.addFormattedLine("Biometric granted", BiometricUtils.isPermissionGranted(context))

        sb.append("\n************ FIRMWARE ************\n")
        sb.addFormattedLine("SDK", Build.VERSION.SDK_INT)
        sb.addFormattedLine("Release", Build.VERSION.RELEASE)
        sb.addFormattedLine("Incremental", Build.VERSION.INCREMENTAL)
        sb.addFormattedLine("Codename", Build.VERSION.CODENAME)
        sb.addFormattedLine("Security patch", Build.VERSION.SECURITY_PATCH)

        return sb.toString()
    }
}