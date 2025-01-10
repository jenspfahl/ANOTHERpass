package de.jepfa.yapm.ui.credential

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.Constants

object DeobfuscationDialog {

    fun openObfuscationDialog(
        context: Context,
        titleText: String,
        messageText: String,
        okText: String,
        cancelText: String,
        handlePassword: (obfuscationKey: Key?) -> Unit) {
        val inputView = LinearLayout(context)
        inputView.orientation = LinearLayout.VERTICAL
        inputView.setPadding(32)

        val pwd1 = EditText(context)
        pwd1.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val filters =
            arrayOf<InputFilter>(InputFilter.LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
        pwd1.filters = filters
        pwd1.hint = context.getString(R.string.enter_codeword)
        pwd1.requestFocus()
        inputView.addView(pwd1)

        val pwd2 = EditText(context)
        pwd2.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        pwd2.setFilters(filters)
        pwd2.hint = context.getString(R.string.repeat_codeword)
        inputView.addView(pwd2)

        val builder = AlertDialog.Builder(context)
        val dialog: AlertDialog = builder
            .setTitle(titleText)
            .setMessage(messageText)
            .setView(inputView)
            .setPositiveButton(okText, null)
            .setNegativeButton(cancelText, null)
            .create()

        dialog.setOnShowListener {
            val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            buttonPositive.setOnClickListener {
                val obfusPasswd1 = Password(pwd1.text)
                val obfusPasswd2 = Password(pwd2.text)
                if (obfusPasswd1.isEmpty()) {
                    pwd1.setError(context.getString(R.string.error_field_required))
                    pwd1.requestFocus()
                    return@setOnClickListener
                }
                else if (obfusPasswd2.isEmpty()) {
                    pwd2.setError(context.getString(R.string.error_field_required))
                    pwd2.requestFocus()
                    return@setOnClickListener
                }
                else if (!obfusPasswd1.isEqual(obfusPasswd2)) {
                    pwd2.setError(context.getString(R.string.password_not_equal))
                    pwd2.requestFocus()
                    return@setOnClickListener
                }

                handlePassword(getKeyFromPasswd(context, obfusPasswd1))

                dialog.dismiss()
            }
            val buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            buttonNegative.setOnClickListener {
                handlePassword(null)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun openDeobfuscationDialogForCredentials(context: Context, handleKey: (obfuscationKey: Key?) -> Unit) {
        openDeobfuscationDialog(
            context,
            R.string.deobfuscate,
            R.string.message_deobfuscate_password,
            handleKey)
    }

    fun openDeobfuscationDialogForMasterPassword(context: Context, handleKey: (obfuscationKey: Key?) -> Unit) {
        openDeobfuscationDialog(
            context,
            R.string.unlock_masterpassword,
            R.string.message_unlock_masterpassword,
            handleKey)
    }

    fun openDeobfuscationDialog(
        context: Context,
        titleTextId: Int,
        messageTextId: Int,
        handleKey: (obfuscationKey: Key?) -> Unit
    ) {
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
        inputText.hint = context.getString(R.string.enter_codeword)
        inputText.requestFocus()
        inputView.addView(inputText)

        val builder = AlertDialog.Builder(context)
        val dialog: AlertDialog = builder
            .setTitle(titleTextId)
            .setMessage(messageTextId)
            .setView(inputView)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            buttonPositive.setOnClickListener {
                val obfusPasswd = Password(inputText.text)
                if (obfusPasswd == null || obfusPasswd.isEmpty()) {
                    inputText.setError(context.getString(R.string.error_field_required))
                    inputText.requestFocus()
                    return@setOnClickListener
                }

                handleKey(getKeyFromPasswd(context, obfusPasswd))
                dialog.dismiss()
            }
            val buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            buttonNegative.setOnClickListener {
                handleKey(null)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun getKeyFromPasswd(context: Context, obfusPasswd: Password): Key {
        val salt = SaltService.getSalt(context)
        val cipherAlgorithm = SecretService.getCipherAlgorithm(context)
        val obfuscationSK =
            SecretService.generateSecretKeyForObfuscation(obfusPasswd, salt, cipherAlgorithm, context)
        return SecretService.secretKeyToKey(obfuscationSK, salt)
    }


}