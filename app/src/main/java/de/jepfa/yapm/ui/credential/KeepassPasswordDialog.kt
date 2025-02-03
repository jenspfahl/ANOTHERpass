package de.jepfa.yapm.ui.credential

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.util.Constants

object KeepassPasswordDialog {

    fun openAskForSettingAPasswordDialog(
        context: Context,
        handlePassword: (password: Password?) -> Unit) {

        val inputView = LinearLayout(context)
        inputView.orientation = LinearLayout.VERTICAL
        inputView.setPadding(32)

        val pwd1 = EditText(context)
        pwd1.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val filters =
            arrayOf<InputFilter>(InputFilter.LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
        pwd1.filters = filters
        pwd1.hint = context.getString(R.string.enter_password)
        pwd1.requestFocus()
        inputView.addView(pwd1)

        val pwd2 = EditText(context)
        pwd2.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        pwd2.setFilters(filters)
        pwd2.hint = context.getString(R.string.repeat_password)
        inputView.addView(pwd2)

        //val checkBox = CheckBox(context)
        //checkBox.text = "Save password for future usage"
        //inputView.addView(checkBox)

        val builder = AlertDialog.Builder(context)
        val dialog: AlertDialog = builder
            .setTitle(R.string.title_takeout_kdbx_master_password)
            .setMessage(R.string.message_takeout_kdbx_master_password)
            .setView(inputView)
            .setPositiveButton(context.getString(android.R.string.ok), null)
            .setNegativeButton(context.getString(android.R.string.cancel), null)
            //.setNeutralButton("GENERATE PASSWORD", null)
            .create()

        dialog.setOnShowListener {
            val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            buttonPositive.setOnClickListener {
                val passwd1 = Password(pwd1.text)
                val passwd2 = Password(pwd2.text)
                if (passwd1.isEmpty()) {
                    pwd1.setError(context.getString(R.string.error_field_required))
                    pwd1.requestFocus()
                    return@setOnClickListener
                }
                else if (passwd1.length < 6) {
                    pwd2.setError(context.getString(R.string.password_too_short))
                    pwd2.requestFocus()
                    return@setOnClickListener
                }
                else if (passwd2.isEmpty()) {
                    pwd2.setError(context.getString(R.string.error_field_required))
                    pwd2.requestFocus()
                    return@setOnClickListener
                }
                else if (!passwd1.isEqual(passwd2)) {
                    pwd2.setError(context.getString(R.string.password_not_equal))
                    pwd2.requestFocus()
                    return@setOnClickListener
                }

                handlePassword(passwd1)

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

    fun openAskForUnlockPasswordDialog(
        context: Context,
        handlePassword: (password: Password?) -> Unit) {
        val inputView = LinearLayout(context)
        inputView.orientation = LinearLayout.VERTICAL
        inputView.setPadding(32)

        val pwd = EditText(context)
        pwd.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val filters =
            arrayOf<InputFilter>(InputFilter.LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
        pwd.filters = filters
        pwd.hint = context.getString(R.string.enter_password)
        pwd.requestFocus()
        inputView.addView(pwd)


        val builder = AlertDialog.Builder(context)
        val dialog: AlertDialog = builder
            .setTitle(R.string.title_enter_kdbx_master_password)
            .setMessage(R.string.message_enter_kdbx_master_password)
            .setView(inputView)
            .setPositiveButton(context.getString(android.R.string.ok), null)
            .setNegativeButton(context.getString(android.R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            buttonPositive.setOnClickListener {
                val passwd = Password(pwd.text)
                if (passwd.isEmpty()) {
                    pwd.setError(context.getString(R.string.error_field_required))
                    pwd.requestFocus()
                    return@setOnClickListener
                }

                handlePassword(passwd)

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


}