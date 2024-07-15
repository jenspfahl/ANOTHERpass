package de.jepfa.yapm.ui.webextension

import android.util.Base64
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.webextension.DeleteDisabledWebExtensionsUseCase
import de.jepfa.yapm.usecase.webextension.DeleteWebExtensionUseCase
import de.jepfa.yapm.util.*
import org.json.JSONObject

object WebExtensionDialogs {

    fun openWebExtensionDetails(webExtension: EncWebExtension, activity: SecureActivity) {

        activity.masterSecretKey?.let { key ->
            val webClientId = SecretService.decryptCommonString(key, webExtension.webClientId)
            val sb = java.lang.StringBuilder()

            val clientPubKeyAsJWK = JSONObject(SecretService.decryptCommonString(key, webExtension.extensionPublicKey))
            val nBase64 = clientPubKeyAsJWK.getString("n")
            val nBytes = Base64.decode(nBase64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val clientPubKeyFingerprint = nBytes.sha256().toHex(separator = ":")

            val serverPublicKey = SecretService.getServerPublicKey(webExtension.getServerKeyPairAlias())
            if (serverPublicKey != null) {
                val m = SecretService.getRsaPublicKeyData(serverPublicKey).first
                val serverPubKeyFingerprint = m.sha256().toHex(separator = ":")
                sb.append("App Public Key Fingerprint:").addNewLine().append(serverPubKeyFingerprint).addNewLine().addNewLine()
            }

            val sharedBaseKey = SecretService.decryptKey(key, webExtension.sharedBaseKey)
            val sharedBaseKeyFingerprint = sharedBaseKey.data.sha256().toHex(separator = ":")
            sharedBaseKey.clear()

            sb.append("Device Public Key Fingerprint:").addNewLine().append(clientPubKeyFingerprint).addNewLine().addNewLine()
            sb.append("Shared Secret Fingerprint:").addNewLine().append(sharedBaseKeyFingerprint).addNewLine().addNewLine()

            AlertDialog.Builder(activity)
                .setTitle("Linking details for $webClientId")
                .setMessage(sb.toString())
                .setIcon(R.drawable.baseline_phonelink_24)
                .setNegativeButton(R.string.close, null)
                .setNeutralButton("Copy to clipboard") { _, _ ->
                    ClipboardUtil.copy(
                        "Linking details for $webClientId",
                        sb.toString(),
                        activity,
                        isSensible = false,
                    )
                    toastText(activity, "Copied to clipboard")
                }
                .show()
        }

    }

        fun openDeleteWebExtension(webExtension: EncWebExtension, activity: SecureActivity, finishActivityAfterDelete: Boolean = false) {
        activity.masterSecretKey?.let { key ->
            val name = if (webExtension.title != null) {
                SecretService.decryptCommonString(key, webExtension.title!!)
            }
            else {
                SecretService.decryptCommonString(key, webExtension.webClientId)
            }
            name

            AlertDialog.Builder(activity)
                .setTitle(R.string.title_delete_web_extension)
                .setMessage(activity.getString(R.string.message_delete_web_extension, name))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                    UseCaseBackgroundLauncher(DeleteWebExtensionUseCase)
                        .launch(activity, webExtension)
                        {
                            if (finishActivityAfterDelete) {
                                activity.finish()
                            }
                        }

                }
                .setNegativeButton(android.R.string.no, null)
                .show()
        }
    }

    fun openDeleteDisabledWebExtension(activity: SecureActivity) {
        activity.masterSecretKey?.let { _ ->


            AlertDialog.Builder(activity)
                .setTitle("Delete disabled linked devices")
                .setMessage("Do you really want to delete all disabled but linked devices?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    UseCaseBackgroundLauncher(DeleteDisabledWebExtensionsUseCase)
                        .launch(activity, Unit)
                        { result ->
                            toastText(activity, "${result.data} disabled links deleted")
                        }

                }
                .setNegativeButton(android.R.string.no, null)
                .show()
        }
    }
}