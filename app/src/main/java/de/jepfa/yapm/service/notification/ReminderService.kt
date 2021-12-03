package de.jepfa.yapm.service.notification

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_MK_EXPORTED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_MK_EXPORT_NOTIFICATION_SHOWED_AS
import de.jepfa.yapm.service.PreferenceService.DATA_MK_EXPORT_NOTIFICATION_SHOWED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_MK_MODIFIED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AS
import de.jepfa.yapm.service.PreferenceService.DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_EXPORTED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_EXPORT_NOTIFICATION_SHOWED_AS
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_EXPORT_NOTIFICATION_SHOWED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_MODIFIED_AT
import de.jepfa.yapm.service.PreferenceService.PREF_REMINDER_PERIOD
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_EXPORT_MK_REMINDER
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_BIOMETRIC_SMP_REMINDER
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_EXPORT_VAULT_REMINDER
import de.jepfa.yapm.service.biometrix.BiometricUtils
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.exportvault.ExportVaultActivity
import de.jepfa.yapm.usecase.secret.ExportEncMasterKeyUseCase
import de.jepfa.yapm.util.toastText
import java.util.*

object ReminderService {


    interface ReminderConfig {
        val prefShowReminder: String
        val dataNotificationShowedAt: String
        val dataNotificationShowedAs: String
        val notificationText: Int
        val notificationAction: Int
        val condition: (SecureActivity) -> Boolean
        val action: (SecureActivity) -> Unit
    }

    object Vault: ReminderConfig {
        override val prefShowReminder = PREF_SHOW_EXPORT_VAULT_REMINDER
        override val dataNotificationShowedAt = DATA_VAULT_EXPORT_NOTIFICATION_SHOWED_AT
        override val dataNotificationShowedAs = DATA_VAULT_EXPORT_NOTIFICATION_SHOWED_AS
        override val notificationText = R.string.export_vault_reminder
        override val notificationAction = R.string.export_vault
        override val condition = { activity: SecureActivity ->
            dateOlderThan(DATA_VAULT_EXPORTED_AT, DATA_VAULT_MODIFIED_AT, activity)
        }
        override val action = { activity: SecureActivity ->
            val intent = Intent(activity, ExportVaultActivity::class.java)
            activity.startActivity(intent)
        }
    }

    object MasterKey: ReminderConfig {
        override val prefShowReminder = PREF_SHOW_EXPORT_MK_REMINDER
        override val dataNotificationShowedAt = DATA_MK_EXPORT_NOTIFICATION_SHOWED_AT
        override val dataNotificationShowedAs = DATA_MK_EXPORT_NOTIFICATION_SHOWED_AS
        override val notificationText = R.string.export_mk_reminder
        override val notificationAction = R.string.export_masterkey
        override val condition = { activity: SecureActivity ->
            dateOlderThan(DATA_MK_EXPORTED_AT, DATA_MK_MODIFIED_AT, activity)
        }
        override val action = { activity: SecureActivity ->
            UseCaseBackgroundLauncher(ExportEncMasterKeyUseCase)
                .launch(activity, Unit)
        }
    }
    object StoredMasterPassword: ReminderConfig {
        override val prefShowReminder = PREF_SHOW_BIOMETRIC_SMP_REMINDER
        override val dataNotificationShowedAt = DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AT
        override val dataNotificationShowedAs = DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AS
        override val notificationText = R.string.biometric_smp_reminder
        override val notificationAction = R.string.show_biometric_smp_howto
        override val condition = { activity: SecureActivity ->
            BiometricUtils.isHardwareSupported(activity)
                    && MasterPasswordService.isMasterPasswordStored(activity)
                    && !MasterPasswordService.isMasterPasswordStoredWithAuth(activity)
        }
        override val action: (SecureActivity) -> Unit = { activity: SecureActivity ->
            AlertDialog.Builder(activity)
                .setTitle(R.string.howto_biometrics_for_smp_title)
                .setMessage(R.string.howto_biometrics_for_smp_text)
                .show()
        }
    }


    fun showReminders(config: ReminderConfig, view: View, activity: SecureActivity): Boolean {
        val remindEnabled = PreferenceService.getAsBool(config.prefShowReminder, activity)
        if (!remindEnabled) {
            return false
        }

        val notificationShowedAt = PreferenceService.getAsDate(config.dataNotificationShowedAt, activity)
        val showAfterSeconds = PreferenceService.getAsInt(PREF_REMINDER_PERIOD, activity)
        if (dateOlderThanSeconds(notificationShowedAt, showAfterSeconds) && config.condition(activity)) {
            val snackBar = Snackbar.make(
                view,
                config.notificationText,
                7_000
            )

            val notificationShowedAs = PreferenceService.getAsBool(config.dataNotificationShowedAs, activity)
            if (notificationShowedAs) {
                snackBar.setAction(R.string.dont_ask_again) {
                    PreferenceService.putBoolean(config.prefShowReminder, false, activity)
                    toastText(activity, R.string.manage_reminder_settings_hint)
                }
            }
            else {
                snackBar.setAction(config.notificationAction) {
                    config.action.invoke(activity)
                }
            }

            snackBar.show()
            PreferenceService.putCurrentDate(config.dataNotificationShowedAt, activity)
            PreferenceService.toggleBoolean(config.dataNotificationShowedAs, activity)

            return true
        }

        return false
    }

    private fun dateOlderThan(
        dataTargetExportedAtKey: String,
        dataTargetModifiedAtKey: String,
        context: Context)
    : Boolean {
        val vaultExportedAt = PreferenceService.getAsDate(dataTargetExportedAtKey, context)
        val vaultModifiedAt = PreferenceService.getAsDate(dataTargetModifiedAtKey, context)
        return dateOlderThan(vaultExportedAt, vaultModifiedAt)
    }

    private fun dateOlderThan(exportedAt: Date?, modifiedAt: Date?): Boolean {
        if (exportedAt == null && modifiedAt != null) {
            return true
        }
        if (exportedAt == null || modifiedAt == null) {
            return false
        }

        return exportedAt.before(modifiedAt)

    }

    private fun dateOlderThanSeconds(date: Date?, seconds: Int): Boolean {
        if (date == null) {
            return true
        }
        val before = System.currentTimeMillis() - (seconds * 1000)
        return date.time < before
    }
}