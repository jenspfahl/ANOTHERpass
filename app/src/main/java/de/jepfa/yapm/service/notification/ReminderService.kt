package de.jepfa.yapm.service.notification

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_MK_EXPORTED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_MK_EXPORT_NOTIFICATION_SHOWED_AS
import de.jepfa.yapm.service.PreferenceService.DATA_MK_EXPORT_NOTIFICATION_SHOWED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_MK_MODIFIED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AS
import de.jepfa.yapm.service.PreferenceService.DATA_BIOMETRIC_SMP_NOTIFICATION_SHOWED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_EXPIRED_PASSWORDS_NOTIFICATION_SHOWED_AS
import de.jepfa.yapm.service.PreferenceService.DATA_EXPIRED_PASSWORDS_NOTIFICATION_SHOWED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_MPT_CREATED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_MP_EXPORTED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_MP_EXPORT_NOTIFICATION_SHOWED_AS
import de.jepfa.yapm.service.PreferenceService.DATA_MP_EXPORT_NOTIFICATION_SHOWED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_MP_MODIFIED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_REFRESH_MPT_NOTIFICATION_SHOWED_AS
import de.jepfa.yapm.service.PreferenceService.DATA_REFRESH_MPT_NOTIFICATION_SHOWED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_EXPORTED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_EXPORT_NOTIFICATION_SHOWED_AS
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_EXPORT_NOTIFICATION_SHOWED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_MODIFIED_AT
import de.jepfa.yapm.service.PreferenceService.PREF_REMINDER_DURATION
import de.jepfa.yapm.service.PreferenceService.PREF_REMINDER_PERIOD
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_EXPORT_MK_REMINDER
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_BIOMETRIC_SMP_REMINDER
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_EXPIRED_PASSWORDS_REMINDER
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_EXPORT_MP_REMINDER
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_EXPORT_VAULT_REMINDER
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_REFRESH_MPT_REMINDER
import de.jepfa.yapm.service.biometrix.BiometricUtils
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.ui.exportvault.ExportVaultActivity
import de.jepfa.yapm.usecase.secret.ExportEncMasterKeyUseCase
import de.jepfa.yapm.usecase.secret.ExportEncMasterPasswordUseCase
import de.jepfa.yapm.usecase.secret.GenerateMasterPasswordTokenUseCase
import de.jepfa.yapm.util.addDays
import de.jepfa.yapm.util.toastText
import java.util.*

object ReminderService {


    interface ReminderConfig {
        val showIt: (SecureActivity) -> Boolean
        val doDeactivate: (SecureActivity) -> Unit
        val dataNotificationShowedAt: String
        val dataNotificationShowedAs: String
        val notificationText: Int
        val notificationAction: Int
        val condition: (SecureActivity) -> Boolean
        val action: (SecureActivity) -> Unit
    }

    object Vault: ReminderConfig {
        override val showIt = { activity: SecureActivity ->
            PreferenceService.getAsBool(PREF_SHOW_EXPORT_VAULT_REMINDER, activity)
        }
        override val doDeactivate: (SecureActivity) -> Unit = { activity: SecureActivity ->
            PreferenceService.putBoolean(PREF_SHOW_EXPORT_VAULT_REMINDER, false, activity)
        }
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
        override val showIt = { activity: SecureActivity ->
            PreferenceService.getAsBool(PREF_SHOW_EXPORT_MK_REMINDER, activity)
        }
        override val doDeactivate: (SecureActivity) -> Unit = { activity: SecureActivity ->
            PreferenceService.putBoolean(PREF_SHOW_EXPORT_MK_REMINDER, false, activity)
        }
        override val dataNotificationShowedAt = DATA_MK_EXPORT_NOTIFICATION_SHOWED_AT
        override val dataNotificationShowedAs = DATA_MK_EXPORT_NOTIFICATION_SHOWED_AS
        override val notificationText = R.string.export_mk_reminder
        override val notificationAction = R.string.action_export_masterkey
        override val condition = { activity: SecureActivity ->
            dateOlderThan(DATA_MK_EXPORTED_AT, DATA_MK_MODIFIED_AT, activity)
        }
        override val action = { activity: SecureActivity ->
            UseCaseBackgroundLauncher(ExportEncMasterKeyUseCase)
                .launch(activity, Unit)
        }
    }

    object MasterPassword: ReminderConfig {
        override val showIt = { activity: SecureActivity ->
            PreferenceService.getAsBool(PREF_SHOW_EXPORT_MP_REMINDER, activity)
        }
        override val doDeactivate: (SecureActivity) -> Unit = { activity: SecureActivity ->
            PreferenceService.putBoolean(PREF_SHOW_EXPORT_MP_REMINDER, false, activity)
        }
        override val dataNotificationShowedAt = DATA_MP_EXPORT_NOTIFICATION_SHOWED_AT
        override val dataNotificationShowedAs = DATA_MP_EXPORT_NOTIFICATION_SHOWED_AS
        override val notificationText = R.string.export_mp_reminder
        override val notificationAction = R.string.action_export_masterpasswd
        override val condition = { activity: SecureActivity ->
            dateOlderThan(DATA_MP_EXPORTED_AT, DATA_MP_MODIFIED_AT, activity)
        }
        override val action: (SecureActivity) -> Unit = { activity: SecureActivity ->
            val encMasterPasswd = Session.getEncMasterPasswd()
            encMasterPasswd?.let {
                ExportEncMasterPasswordUseCase.startUiFlow(activity, encMasterPasswd, noSessionCheck = false)
            }
        }
    }

    object StoredMasterPassword: ReminderConfig {
        override val showIt = { activity: SecureActivity ->
            PreferenceService.getAsBool(PREF_SHOW_BIOMETRIC_SMP_REMINDER, activity)
        }
        override val doDeactivate: (SecureActivity) -> Unit = { activity: SecureActivity ->
            PreferenceService.putBoolean(PREF_SHOW_BIOMETRIC_SMP_REMINDER, false, activity)
        }
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

    object RefreshMasterPasswordToken: ReminderConfig {
        override val showIt = { activity: SecureActivity ->
            PreferenceService.getAsInt(PREF_SHOW_REFRESH_MPT_REMINDER, activity) != 0
        }
        override val doDeactivate: (SecureActivity) -> Unit = { activity: SecureActivity ->
            PreferenceService.putInt(PREF_SHOW_REFRESH_MPT_REMINDER, 0, activity)
        }
        override val dataNotificationShowedAt = DATA_REFRESH_MPT_NOTIFICATION_SHOWED_AT
        override val dataNotificationShowedAs = DATA_REFRESH_MPT_NOTIFICATION_SHOWED_AS
        override val notificationText = R.string.refresh_mpt_reminder
        override val notificationAction = R.string.show_refresh_mpt
        override val condition = { activity: SecureActivity ->
            val hasMpt = PreferenceService.isPresent(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, activity)
            val createdDate = PreferenceService.getAsDate(DATA_MPT_CREATED_AT, activity)
            val duration = PreferenceService.getAsInt(PREF_SHOW_REFRESH_MPT_REMINDER, activity)
            if (hasMpt && createdDate != null && duration != 0) {
                val shouldRenewedDate = createdDate.addDays(duration)
                val now = Date()
                dateOlderThan(shouldRenewedDate, now)
            }
            else {
                false
            }
        }
        override val action: (SecureActivity) -> Unit = { activity: SecureActivity ->
            GenerateMasterPasswordTokenUseCase.openDialog(activity) {}
        }
    }

    object ExpiredPasswords: ReminderConfig {
        override val showIt = { activity: SecureActivity ->
            PreferenceService.getAsBool(PREF_SHOW_EXPIRED_PASSWORDS_REMINDER, activity)
        }
        override val doDeactivate: (SecureActivity) -> Unit = { activity: SecureActivity ->
            PreferenceService.putBoolean(PREF_SHOW_EXPIRED_PASSWORDS_REMINDER, false, activity)
        }
        override val dataNotificationShowedAt = DATA_EXPIRED_PASSWORDS_NOTIFICATION_SHOWED_AT
        override val dataNotificationShowedAs = DATA_EXPIRED_PASSWORDS_NOTIFICATION_SHOWED_AS
        override val notificationText = R.string.expired_passwords_reminder
        override val notificationAction = R.string.show_expired_passwords
        override val condition = { activity: SecureActivity ->
            if (!Session.isDenied()) {
                activity.credentialViewModel.hasExpiredCredentials()
            }
            else {
                false
            }
        }
        override val action: (SecureActivity) -> Unit = { activity: SecureActivity ->
            if (activity is ListCredentialsActivity) {
                activity.searchForExpiredCredentials()
            }
        }
    }

    private val configs = listOf(
        ExpiredPasswords,
        MasterPassword,
        Vault,
        MasterKey,
        StoredMasterPassword ,
        RefreshMasterPasswordToken)


    private fun checkAndShowReminder(config: ReminderConfig, view: View, activity: SecureActivity, showNow: Boolean = false): Boolean {
        val remindEnabled = config.showIt(activity)
        if (!remindEnabled) {
            return false
        }

        val notificationShowedAt = PreferenceService.getAsDate(config.dataNotificationShowedAt, activity)
        val showAfterSeconds = if (showNow) 0 else PreferenceService.getAsInt(PREF_REMINDER_PERIOD, activity)
        if (dateOlderThanSeconds(notificationShowedAt, showAfterSeconds) && config.condition(activity)) {

            showReminder(view, config, activity, showAlwaysRealAction = showNow)
            PreferenceService.putCurrentDate(config.dataNotificationShowedAt, activity)

            return true
        }

        return false
    }

    private fun showReminder(
        view: View,
        config: ReminderConfig,
        activity: SecureActivity,
        showAlwaysRealAction: Boolean = false
    ) {
        val snackBar = Snackbar.make(
            view,
            config.notificationText,
            PreferenceService.getAsInt(PREF_REMINDER_DURATION, view.context),
        )

        snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            ?.apply {
                maxLines = 3
            }

        val notificationShowedAs =
            PreferenceService.getAsBool(config.dataNotificationShowedAs, activity)
        if (notificationShowedAs && !showAlwaysRealAction) {
            snackBar.setAction(R.string.dont_ask_again) {
                config.doDeactivate(activity)
                toastText(activity, R.string.manage_reminder_settings_hint)
                PreferenceService.putBoolean(config.dataNotificationShowedAs, false, activity)
            }
        } else {
            snackBar.setAction(config.notificationAction) {
                config.action.invoke(activity)
            }
            // next action should be "don't show again", but only if sen multiple times
            if (!showAlwaysRealAction && Random().nextBoolean()) {
                PreferenceService.putBoolean(config.dataNotificationShowedAs, true, activity)
            }
        }

        snackBar.show()
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

    private fun dateOlderThan(dateA: Date?, dateB: Date?): Boolean {
        if (dateA == null && dateB != null) {
            return true
        }
        if (dateA == null || dateB == null) {
            return false
        }

        return dateA.before(dateB)

    }

    private fun dateOlderThanSeconds(date: Date?, seconds: Int): Boolean {
        if (date == null) {
            return true
        }
        val before = System.currentTimeMillis() - (seconds * 1000)
        return date.time < before
    }

    fun showNextReminder(view: View, activity: SecureActivity, showNow: Boolean = false): Boolean {
        val sorted = configs.sortedBy { PreferenceService.getAsDate(it.dataNotificationShowedAt, activity) }
        for (config in sorted) {
            val shown = checkAndShowReminder(config, view, activity, showNow)
            if (shown) return true
        }
        return false
    }

    fun hasNextReminder(activity: SecureActivity): Boolean {
        return configs
            .filter { it.showIt(activity) }
            .filter { it.condition(activity) }
            .size > 1
    }

    fun showLastReminder(view: View, activity: SecureActivity): Boolean {

        val lastShownConfig = getLastReminder(activity)


        return if (lastShownConfig != null) {
            showReminder(view, lastShownConfig, activity, showAlwaysRealAction = true)
            true
        } else {
            false
        }
    }

    fun hasLastReminder(activity: SecureActivity) = getLastReminder(activity) != null

    private fun getLastReminder(activity: SecureActivity) = configs
        .filter { it.showIt(activity) }
        .filter { it.condition(activity) }
        .maxByOrNull {
            PreferenceService.getAsDate(it.dataNotificationShowedAt, activity) ?: Date(0)
        }


}