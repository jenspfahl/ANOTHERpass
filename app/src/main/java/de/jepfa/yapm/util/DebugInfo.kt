package de.jepfa.yapm.util

import android.content.Context
import android.os.Build
import de.jepfa.yapm.BuildConfig
import de.jepfa.yapm.database.YapmDatabase
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_CIPHER_ALGORITHM
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_VERSION
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.Constants.UNKNOWN_VAULT_VERSION

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

    fun getDebugInfo(context: Context): String {
        val sb = StringBuilder()
        sb.append("\n************ APP INFORMATION ***********\n")
        sb.addFormattedLine("Version", getVersionName(context))
        sb.addFormattedLine("VersionCode", getVersionCode(context))
        sb.addFormattedLine("Database Version", YapmDatabase.getVersion())
        sb.addFormattedLine("Vault Version", PreferenceService.getAsString(DATA_VAULT_VERSION, context) ?: UNKNOWN_VAULT_VERSION)
        sb.addFormattedLine("Vault Cipher", SecretService.getCipherAlgorithm(context))
        sb.addFormattedLine("BuildTimestamp", Constants.SDF_DT_MEDIUM.format(BuildConfig.BUILD_TIME))
        sb.addFormattedLine("BuildType", BuildConfig.BUILD_TYPE)

        sb.append("\n************ DEVICE INFORMATION ***********\n")
        sb.addFormattedLine("Brand", Build.BRAND)
        sb.addFormattedLine("Device", Build.DEVICE)
        sb.addFormattedLine("Model", Build.MODEL)
        sb.addFormattedLine("Id", Build.ID)
        sb.addFormattedLine("Product", Build.PRODUCT)
        sb.addFormattedLine("NFC available", NfcService.isNfcAvailable(context))
        sb.addFormattedLine("NFC enabled", NfcService.isNfcEnabled(context))

        sb.append("\n************ PERMISSIONS ************\n")
        sb.addFormattedLine("Read Storage granted", PermissionChecker.hasReadStoragePermissions(context))
        sb.addFormattedLine("Read/write storage granted", PermissionChecker.hasRWStoragePermissions(context))
        sb.addFormattedLine("Overlay dialog granted", PermissionChecker.hasOverlayPermission(context))
        sb.addFormattedLine("Camera granted", PermissionChecker.hasCameraPermission(context))

        sb.append("\n************ FIRMWARE ************\n")
        sb.addFormattedLine("SDK", Build.VERSION.SDK)
        sb.addFormattedLine("Release", Build.VERSION.RELEASE)
        sb.addFormattedLine("Incremental", Build.VERSION.INCREMENTAL)

        return sb.toString()
    }
}