package de.jepfa.yapm.util

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.util.Log
import de.jepfa.yapm.BuildConfig
import de.jepfa.yapm.database.YapmDatabase
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_VERSION
import de.jepfa.yapm.service.biometrix.BiometricUtils
import de.jepfa.yapm.service.net.HttpServer.SERVER_LOG_PREFIX
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.Constants.INITIAL_VAULT_VERSION
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import java.io.*

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
        val vaultCreatedAt = PreferenceService.getAsDate(
            PreferenceService.DATA_VAULT_CREATED_AT,
            context
        )
        val vaultImportedAt = PreferenceService.getAsDate(
            PreferenceService.DATA_VAULT_IMPORTED_AT,
            context
        )

        val anonymizedVaultId = SaltService.getAnonymizedVaultId(context)

        val sb = StringBuilder()
        sb.append("\n************ APP INFORMATION ***********\n")
        sb.addFormattedLine("Version", getVersionName(context))
        sb.addFormattedLine("Version Code", getVersionCode(context))
        sb.addFormattedLine("Database Version", YapmDatabase.getVersion())
        sb.addFormattedLine("Vault Id (anonymized)", anonymizedVaultId)
        sb.addFormattedLine("Vault Version", PreferenceService.getAsString(DATA_VAULT_VERSION, context) ?: INITIAL_VAULT_VERSION)
        sb.addFormattedLine("Vault Cipher", SecretService.getCipherAlgorithm(context))
        if (vaultCreatedAt != null) {
            sb.addFormattedLine("Vault Created At", dateTimeToNiceString(vaultCreatedAt, context))
        }
        if (vaultImportedAt != null) {
            sb.addFormattedLine("Vault Imported At", dateTimeToNiceString(vaultImportedAt, context))
        }
        sb.addFormattedLine("MP stored", MasterPasswordService.isMasterPasswordStored(context))
        sb.addFormattedLine("MP stored with auth", MasterPasswordService.isMasterPasswordStoredWithAuth(context))
        sb.addFormattedLine("Build Timestamp", BuildConfig.BUILD_TIME.toSimpleDateTimeFormat())
        sb.addFormattedLine("Build Type", BuildConfig.BUILD_TYPE)
        sb.addFormattedLine("Debug Mode", isDebug)

        sb.append("\n************ DEVICE INFORMATION ***********\n")
        sb.addFormattedLine("Brand", Build.BRAND)
        sb.addFormattedLine("Manufacturer", Build.MANUFACTURER)
        sb.addFormattedLine("Device", Build.DEVICE)
        sb.addFormattedLine("Model", Build.MODEL)
        sb.addFormattedLine("Product", Build.PRODUCT)
        sb.addFormattedLine("Hardware", Build.HARDWARE)
        //sb.addFormattedLine("OS Build Id", Build.ID)
        sb.addFormattedLine("NFC available", NfcService.isNfcAvailable(context))
        sb.addFormattedLine("NFC enabled", NfcService.isNfcEnabled(context))
        sb.addFormattedLine("Has StrongBox support", SecretService.hasStrongBoxSupport(context) ?: "-")
        sb.addFormattedLine("Has hardware backed TEE", SecretService.hasHardwareTEESupport(context) ?: "-")
        sb.addFormattedLine("Has biometrics support", BiometricUtils.isBiometricsSupported(context))
        sb.addFormattedLine("Has biometrics enrolled", BiometricUtils.hasBiometricsEnrolled(context))
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        sb.addFormattedLine("Is device lock enabled", keyguardManager.isDeviceSecure)


        sb.append("\n************ PERMISSIONS ************\n")
        sb.addFormattedLine("Read Storage granted", PermissionChecker.hasReadStoragePermissions(context))
        sb.addFormattedLine("Read/write storage granted", PermissionChecker.hasRWStoragePermissions(context))
        sb.addFormattedLine("Notifications granted", PermissionChecker.hasNotificationPermission(context))
        sb.addFormattedLine("Overlay dialog granted", PermissionChecker.hasOverlayPermission(context))
        sb.addFormattedLine("Camera granted", PermissionChecker.hasCameraPermission(context))
        sb.addFormattedLine("Biometric granted", BiometricUtils.isPermissionGranted(context))

        sb.append("\n************ FIRMWARE ************\n")
        sb.addFormattedLine("SDK", Build.VERSION.SDK_INT)
        sb.addFormattedLine("Release", Build.VERSION.RELEASE)
        sb.addFormattedLine("Incremental", Build.VERSION.INCREMENTAL)
        sb.addFormattedLine("Codename", Build.VERSION.CODENAME)
        sb.addFormattedLine("Security patch", Build.VERSION.SECURITY_PATCH)

        sb.append("\n************ APP-ERROR-LOG ************\n")
        val logs = getLogcat("", command = "-b all *:E", null)?.takeLast(4096 * 2)
        sb.append(logs)

        return sb.toString()
    }

    fun getDebugLog(context: Context): String {
        return getLogcat("", command = "-b all *:D")?:"no logs available"
    }

    fun getServerLog(context: Context): String {
        var logs = getLogcat(
            SERVER_LOG_PREFIX,
            command = "-v tag *:I",
            cutoutPrefix = "I/$SERVER_LOG_PREFIX: "
        )
        return logs ?:"no logs available"
    }

    private fun getLogcat(filter: String, command: String = "-v time *:I", cutoutPrefix: String? = null): String? {
        try {
            val p = Runtime.getRuntime().exec("logcat -d $command") // only errors
            BufferedReader(InputStreamReader(p.inputStream)).use { bais ->
                StringWriter().use { sw ->
                    PrintWriter(sw).use { pw ->
                        var line: String?
                        while (bais.readLine().also { line = it } != null) {
                            if (line?.contains(filter) == true) {
                                if (cutoutPrefix != null) {
                                    line = line?.removePrefix(cutoutPrefix)
                                }
                                pw.println(line)
                            }
                        }
                        return sw.toString()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(LOG_PREFIX + "Debug","cannot gather logs", e)
        }
        return null
    }

    fun clearLogs() {
        try {
            Runtime.getRuntime().exec("logcat -b all -c") // clear logs
        } catch (e: IOException) {
            Log.e(LOG_PREFIX + "Debug","cannot clear logs", e)
        }
    }
}