package de.jepfa.yapm.usecase.vault

import android.net.Uri
import androidx.core.net.toUri
import de.jepfa.yapm.service.io.TempFileService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import de.jepfa.yapm.util.Constants
import java.util.*

object ShareVaultUseCase: UseCase<ShareVaultUseCase.Input, Uri?, SecureActivity> {

    data class Input(val includeMasterKey: Boolean,
                     val includeSettings: Boolean)

    override fun execute(input: Input, activity: SecureActivity): UseCaseOutput<Uri?> {
        activity.masterSecretKey?.let{ key ->

            var tempFile = TempFileService.createTempFile(activity, getBackupFileName())

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

    fun getBackupFileName(): String {
        return "anotherpassvault-${Constants.SDF_D_INTERNATIONAL.format(Date())}.json"
    }

}