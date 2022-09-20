package de.jepfa.yapm.usecase.vault

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.io.CsvService
import de.jepfa.yapm.service.io.TempFileService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.OutputUseCase
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.toastText
import java.util.*

object ExportPlainCredentialsUseCase: OutputUseCase<Uri?, SecureActivity>() {


    override fun execute(activity: SecureActivity): UseCaseOutput<Uri?> {
        activity.masterSecretKey?.let{ key ->

            var tempFile = TempFileService.createTempFile(activity, getCsvFileName(activity))

            val csvData = CsvService.createCsvExportContent(
                activity.getApp().credentialRepository.getAllSync(), Session.getMasterKeySK())

            var success = false
            if (csvData != null) {
                success = CsvService.writeCsvExportFile(activity, tempFile.toUri(), csvData)
            }

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
        toastText(activity, R.string.cannot_share_csvfile)
    }

    fun getCsvFileName(context: Context): String {
        val currentDate = Constants.SDF_D_INTERNATIONAL.format(Date())
        val vaultId = SaltService.getVaultId(context)
        return "anotherpass_credentials-${vaultId}-${currentDate}.csv"
    }

    fun getSubject(context: Context): String {
        val currentDate = Constants.SDF_D_INTERNATIONAL.format(Date())
        val vaultId = SaltService.getVaultId(context)
        return "ANOTHERpass credentials (vault id = '${vaultId}') takeout from $currentDate"
    }

}