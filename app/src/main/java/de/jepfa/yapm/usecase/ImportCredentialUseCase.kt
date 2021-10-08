package de.jepfa.yapm.usecase

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.widget.Toast
import com.pchmn.materialchips.R2.attr.showText
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.toastText

object ImportCredentialUseCase {
    fun execute(credential: EncCredential, activity: SecureActivity) {
        val credentialId = credential.id
        if (credentialId != null) {
            activity.credentialViewModel.findById(credentialId).observe(activity) { existingCredential ->
                if (existingCredential != null) {
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.import_credential)
                        .setMessage(R.string.credential_already_exists)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->

                            credential.id = existingCredential.id //TODO mapp here to existing
                            saveAndNavigateBack(insert = false, activity, credential)
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
                }
                else {
                    saveAndNavigateBack(insert = true, activity, credential)
                }
            }
        }
        else {
            saveAndNavigateBack(insert = true, activity, credential)
        }
    }

    private fun saveAndNavigateBack(
        insert: Boolean,
        activity: SecureActivity,
        credential: EncCredential
    ) {
        if (insert) activity.credentialViewModel.insert(credential)
        else activity.credentialViewModel.update(credential)

        toastText(activity, "Credential imported")
        val upIntent = Intent(
            activity,
            ListCredentialsActivity::class.java
        ) //TODO dont do it if abort insert/update
        activity.navigateUpTo(upIntent)
    }

}