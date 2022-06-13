package de.jepfa.yapm.usecase.vault

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_EXPORTED_AT
import de.jepfa.yapm.service.PreferenceService.DATA_VAULT_MODIFIED_AT
import de.jepfa.yapm.service.PreferenceService.STATE_LOGIN_DENIED_AT
import de.jepfa.yapm.service.PreferenceService.STATE_PREVIOUS_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.PreferenceService.STATE_PREVIOUS_LOGIN_SUCCEEDED_AT
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.*

object ShowVaultInfoUseCase: InputUseCase<ShowVaultInfoUseCase.Input, SecureActivity>() {

    data class Input(val credentialCount: Int, val labelCount: Int)

    override fun doExecute(input: Input, activity: SecureActivity): Boolean {

        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val icon: Drawable = activity.applicationInfo.loadIcon(activity.packageManager)

        val vaultId = SaltService.getVaultId(activity)
        val vaultVersion = PreferenceService.getAsString(
            PreferenceService.DATA_VAULT_VERSION,
            activity
        ) ?: Constants.INITIAL_VAULT_VERSION
        val vaultCreatedAt = PreferenceService.getAsDate(
            PreferenceService.DATA_VAULT_CREATED_AT,
            activity
        )
        val vaultImportedAt = PreferenceService.getAsDate(
            PreferenceService.DATA_VAULT_IMPORTED_AT,
            activity
        )
        val cipherAlgorithm = SecretService.getCipherAlgorithm(activity)

        val sb = StringBuilder()
        sb.addFormattedLine(activity.getString(R.string.vault_id), vaultId)
        sb.addFormattedLine(activity.getString(R.string.vault_version), vaultVersion)
        sb.addFormattedLine(activity.getString(R.string.cipher_name), activity.getString(cipherAlgorithm.uiLabel))
        sb.addNewLine()
        sb.addFormattedLine(activity.getString(R.string.credential_count), input.credentialCount)
        sb.addFormattedLine(activity.getString(R.string.label_count), input.labelCount)
        sb.addNewLine()
        if (vaultCreatedAt != null) {
            sb.addFormattedLine(activity.getString(R.string.vault_created_at), dateToNiceString(vaultCreatedAt, activity))
        }
        if (vaultImportedAt != null) {
            sb.addFormattedLine(activity.getString(R.string.vault_imported_at), dateToNiceString(vaultImportedAt, activity))
        }
        val vaultModifiedAt = PreferenceService.getAsDate(DATA_VAULT_MODIFIED_AT, activity)
        vaultModifiedAt?.let {
            sb.addFormattedLine(activity.getString(R.string.vault_modified_at), dateToNiceString(it, activity))
        }
        val vaultExportedAt = PreferenceService.getAsDate(DATA_VAULT_EXPORTED_AT, activity)
        vaultExportedAt?.let {
            sb.addFormattedLine(activity.getString(R.string.vault_exported_at), dateToNiceString(it, activity))
        }
        sb.addNewLine()
        val previousLoginSucceededAt = PreferenceService.getAsDate(STATE_PREVIOUS_LOGIN_SUCCEEDED_AT, activity)
        previousLoginSucceededAt?.let {
            sb.addFormattedLine(activity.getString(R.string.previous_login_at), dateToNiceString(it, activity))
        }
        val lastDeniedLoginAt = PreferenceService.getAsDate(STATE_LOGIN_DENIED_AT, activity)
        lastDeniedLoginAt?.let {
            sb.addFormattedLine(activity.getString(R.string.last_denied_login_at), dateToNiceString(it, activity))
        }
        val lastDeniedLoginAttempts = PreferenceService.getAsInt(STATE_PREVIOUS_LOGIN_ATTEMPTS, activity)
        lastDeniedLoginAttempts?.let {
            sb.addFormattedLine(activity.getString(R.string.last_denied_login_attempts), it)
        }

        builder.setTitle(R.string.vault_info)
            .setMessage(sb.toString())
            .setIcon(icon)
            .show()

        return true
    }

}