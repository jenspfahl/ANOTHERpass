package de.jepfa.yapm.usecase.vault

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.widget.EditText
import androidx.core.net.toUri
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.CsvService
import de.jepfa.yapm.service.io.KdbxService
import de.jepfa.yapm.service.io.TempFileService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.OutputUseCase
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.usecase.credential.ExportCredentialUseCase.ExportMode
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.toastText
import java.io.ByteArrayOutputStream
import java.util.*



object ExportPlainCredentialsUseCase: UseCase<ExportPlainCredentialsUseCase.Input, Uri?, SecureActivity> {

    data class Input(val currentPin: Password, val keepassPassword: Password?)

    override suspend fun execute(input: Input, activity: SecureActivity): UseCaseOutput<Uri?> {

        val errorMessage = checkPin(input.currentPin, activity)
        if (errorMessage != null) {
            return UseCaseOutput.fail(errorMessage)
        }

        activity.masterSecretKey?.let{ key ->

            if (input.keepassPassword != null) {
                val tempFile =
                    TempFileService.createTempFile(activity, getTakeoutFileName(activity, "kdbx"))

                val byteStream = ByteArrayOutputStream()
                var success = KdbxService.createKdbxExportContent(
                    input.keepassPassword,
                    activity.getApp().credentialRepository.getAllSync(),
                    key,
                    byteStream,
                    activity
                )

                if (success) {
                    success = FileUtil.writeFile(activity, tempFile.toUri(), byteStream)
                }

                if (success) {
                    val uri = TempFileService.getContentUriFromFile(activity, tempFile)
                    return UseCaseOutput(uri)
                }
            }
            else {
                val tempFile =
                    TempFileService.createTempFile(activity, getTakeoutFileName(activity, "csv"))

                val csvData = CsvService.createCsvExportContent(
                    activity.getApp().credentialRepository.getAllSync(), Session.getMasterKeySK()
                )

                var success = false
                if (csvData != null) {
                    success = CsvService.writeCsvExportFile(activity, tempFile.toUri(), csvData)
                }

                if (success) {
                    val uri = TempFileService.getContentUriFromFile(activity, tempFile)
                    return UseCaseOutput(uri)
                }
            }
        }
        return UseCaseOutput.fail(activity.getString(R.string.something_went_wrong))
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(receiver, IntentFilter(shareAction), RECEIVER_EXPORTED)

            }
            else {
                activity.registerReceiver(receiver, IntentFilter(shareAction))
            }

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

    fun getTakeoutFileName(context: Context, extension: String): String {
        val currentDate = Constants.SDF_D_INTERNATIONAL.format(Date())
        val vaultId = SaltService.getVaultId(context)
        return "anotherpass_credentials-${vaultId}-${currentDate}.$extension"
    }



    private fun getSubject(context: Context): String {
        val currentDate = Constants.SDF_D_INTERNATIONAL.format(Date())
        val vaultId = SaltService.getVaultId(context)
        return "ANOTHERpass credentials (vault id = '${vaultId}') takeout from $currentDate"
    }

    fun checkPin(currentPin: Password, context: Context): String? {

        if (currentPin.isEmpty()) {
            return context.getString(R.string.pin_required)
        }

        val encEncryptedMasterKey =
            PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, context)
                ?: return context.getString(R.string.something_went_wrong)

        val currentMasterPassword = MasterPasswordService.getMasterPasswordFromSession(context)
            ?: return context.getString(R.string.something_went_wrong)

        val cipherAlgorithm = SecretService.getCipherAlgorithm(context)
        val salt = SaltService.getSalt(context)
        val masterPassphraseSK = MasterKeyService.getMasterPassPhraseSK(
            currentPin,
            currentMasterPassword,
            salt,
            cipherAlgorithm,
            context,
        )

        MasterKeyService.getMasterKey(masterPassphraseSK, encEncryptedMasterKey, context)
                ?: return context.getString(R.string.pin_wrong)
        return null
    }

}