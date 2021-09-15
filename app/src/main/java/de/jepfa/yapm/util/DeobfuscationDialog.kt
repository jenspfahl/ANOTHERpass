package de.jepfa.yapm.util

import android.app.AlertDialog
import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.setPadding
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService

object DeobfuscationDialog {
    fun openDeobfuscationDialog(context: Context, handlePassword: (obfuscationKey: Key) -> Unit) {

        val inputView = LinearLayout(context)
        inputView.orientation = LinearLayout.VERTICAL
        inputView.setPadding(32)

        val inputText = EditText(context)
        inputText.setPadding(32)
        inputText.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val filters =
            arrayOf<InputFilter>(InputFilter.LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
        inputText.filters = filters
        inputText.requestFocus()
        inputView.addView(inputText)

        val builder = AlertDialog.Builder(context)
        val dialog: AlertDialog = builder
            .setTitle(R.string.deobfuscate)
            .setMessage(R.string.message_deobfuscate_password)
            .setView(inputView)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            buttonPositive.setOnClickListener {
                val obfusPasswd = Password.fromEditable(inputText.text)
                if (obfusPasswd == null || obfusPasswd.isEmpty()) {
                    inputText.setError(context.getString(R.string.error_field_required))
                    inputText.requestFocus()
                    return@setOnClickListener
                }

                val salt = SaltService.getSalt(context)
                val cipherAlgorithm = SecretService.getCipherAlgorithm(context)
                val obfuscationKey = SecretService.generateNormalSecretKey(obfusPasswd, salt, cipherAlgorithm)
                handlePassword(SecretService.secretKeyToKey(obfuscationKey, salt))
                dialog.dismiss()
            }
            val buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            buttonNegative.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.show()
    }

}