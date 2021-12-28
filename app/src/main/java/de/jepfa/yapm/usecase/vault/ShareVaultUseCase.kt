package de.jepfa.yapm.usecase.vault

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.net.toUri
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.TempFileService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.toastText
import java.util.*

object ShareVaultUseCase: UseCase<ShareVaultUseCase.Input, Uri?, SecureActivity> {

    data class Input(val includeMasterKey: Boolean,
                     val includeSettings: Boolean)

    override fun execute(input: Input, activity: SecureActivity): UseCaseOutput<Uri?> {
        activity.masterSecretKey?.let{ key ->

            var tempFile = TempFileService.createTempFile(activity, getBackupFileName(activity))

            val success =
                VaultExportService.createVaultFile(
                    activity,
                    activity.getApp(),
                    input.includeMasterKey, input.includeSettings, tempFile.toUri())

            if (success) {
                val uri = TempFileService.getContentUriFromFile(activity, tempFile)
                return UseCaseOutput(uri)
            }
        }
        return UseCaseOutput(false, null)
    }

    fun startShareActivity(uri: Uri?, activity: SecureActivity) {
        if (uri != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.setDataAndType(uri, activity.contentResolver.getType(uri))
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.putExtra(
                Intent.EXTRA_SUBJECT,
                getSubject(activity)
            )

            val receiver = object: BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    context?.let {
                        context.unregisterReceiver(this)
                        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_EXPORTED_AT, activity)
                        val upIntent = Intent(activity.intent)
                        activity.navigateUpTo(upIntent)
                    }
                }
            }

            val shareAction = "de.jepfa.yapm.share.SHARE_ACTION_" + UUID.randomUUID().toString()
            val receiverIntent = Intent(shareAction)

            activity.registerReceiver(receiver, IntentFilter(shareAction))

            val intentSender = PendingIntent
                .getBroadcast(activity, 0, receiverIntent, PendingIntent.FLAG_IMMUTABLE)
                .intentSender

            activity.startActivity(
                Intent.createChooser(
                    shareIntent,
                    activity.getString(R.string.send_to),
                    intentSender
                )
            )
            return
        }
        toastText(activity, R.string.cannot_share_vault)
    }

    fun getBackupFileName(context: Context): String {
        val currentDate = Constants.SDF_D_INTERNATIONAL.format(Date())
        val vaultId = SaltService.getVaultId(context)
        return "anotherpassvault-${vaultId}-${currentDate}.json"
    }

    fun getSubject(context: Context): String {
        val currentDate = Constants.SDF_D_INTERNATIONAL.format(Date())
        val vaultId = SaltService.getVaultId(context)
        return "ANOTHERpass vault '${vaultId}' backup from $currentDate"
    }

}