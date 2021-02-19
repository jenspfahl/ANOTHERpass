package de.jepfa.yapm.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.secretgenerator.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import de.jepfa.yapm.ui.MainActivity as MainActivity1

class NewCredentialActivity : AppCompatActivity() {

    private val PASSWD_STRENGTH_DEFAULT = PasswordStrength.STRONG

    private lateinit var editCredentialNameView: EditText
    private lateinit var editCredentialAdditionalInfoView: EditText
    private lateinit var generatedPasswdView: TextView
    private lateinit var switchUpperCaseChar: Switch
    private lateinit var switchAddDigit: Switch
    private lateinit var switchAddSpecialChar: Switch
    private lateinit var radioStrength: RadioGroup

    private var generatedPassword: Password = Password("")
    private var currentId: Int = -1

    private val secretService = SecretService() // TODO better service resolution
    private val passphraseGenerator = PassphraseGenerator()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_credential)
        editCredentialNameView = findViewById(R.id.edit_credential_name)
        editCredentialAdditionalInfoView = findViewById(R.id.edit_credential_additional_info)
        generatedPasswdView = findViewById(R.id.generated_passwd)
        radioStrength = findViewById(R.id.radio_strengths)

        val idExtra = intent.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)
        if (idExtra != -1) {
            val nameExtra = intent.getStringExtra(EncCredential.EXTRA_CREDENTIAL_NAME)
            val addInfoExtra = intent.getStringExtra(EncCredential.EXTRA_CREDENTIAL_ADDITIONAL_INFO)
            val passwdExtra = intent.getStringExtra(EncCredential.EXTRA_CREDENTIAL_PASSWORD)
            val originCredential = EncCredential(idExtra, nameExtra, addInfoExtra, passwdExtra, false)

            currentId = idExtra
            val key = secretService.getAndroidSecretKey("test-key")
            val name = secretService.decryptCommonString(key, originCredential.name)
            val additionalInfo = secretService.decryptCommonString(key, originCredential.additionalInfo)
            val password = secretService.decryptPassword(key, originCredential.password)
            editCredentialNameView.setText(name)
            editCredentialAdditionalInfoView.setText(additionalInfo)
            generatedPassword = password
            generatedPasswdView.setText(password.debugToString())
        }

        val radioStrengthStrong: RadioButton = findViewById(R.id.radio_strength_strong)
        val radioStrengthSuperStrong: RadioButton = findViewById(R.id.radio_strength_super_strong)
        val radioStrengthExtreme: RadioButton = findViewById(R.id.radio_strength_extreme)

        buildRadioButton(radioStrengthStrong, PasswordStrength.STRONG)
        buildRadioButton(radioStrengthSuperStrong, PasswordStrength.SUPER_STRONG)
        buildRadioButton(radioStrengthExtreme, PasswordStrength.EXTREME)

        switchUpperCaseChar = findViewById(R.id.switch_upper_case_char)
        switchAddDigit = findViewById(R.id.switch_add_digit)
        switchAddSpecialChar = findViewById(R.id.switch_add_special_char)

        val buttonGeneratePasswd: Button = findViewById(R.id.button_generate_passwd)
        buttonGeneratePasswd.setOnClickListener(View.OnClickListener {
            generatedPassword = generatePassword()
            generatedPasswdView.text = generatedPassword.debugToString()
        })

        val button = findViewById<Button>(R.id.button_save)
        button.setOnClickListener {
            if (generatedPassword.data.isEmpty()) {
                Toast.makeText(it.context, "Generate a password first", Toast.LENGTH_LONG).show()
            }
            else {
                val replyIntent = Intent()
                if (TextUtils.isEmpty(editCredentialNameView.text)) {
                    editCredentialNameView.setError(getString(R.string.error_field_required))
                    editCredentialNameView.requestFocus()
                } else {
                    val key = secretService.getAndroidSecretKey("test-key")

                    val name = editCredentialNameView.text.toString()
                    val additionalInfo = editCredentialAdditionalInfoView.text.toString()

                    val encName = secretService.encryptCommonString(key, name)
                    val encAdditionalInfo = secretService.encryptCommonString(key, additionalInfo)
                    val encPassword = secretService.encryptPassword(key, generatedPassword)
                    generatedPassword.clear()

                    replyIntent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, currentId)
                    replyIntent.putExtra(EncCredential.EXTRA_CREDENTIAL_NAME, encName.toBase64String())
                    replyIntent.putExtra(EncCredential.EXTRA_CREDENTIAL_ADDITIONAL_INFO, encAdditionalInfo.toBase64String())
                    replyIntent.putExtra(EncCredential.EXTRA_CREDENTIAL_PASSWORD, encPassword.toBase64String())
                    setResult(Activity.RESULT_OK, replyIntent)
                    finish()
                }
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun generatePassword() : Password {
        val passwordStrength = when (radioStrength.checkedRadioButtonId) {
            R.id.radio_strength_strong -> PasswordStrength.STRONG
            R.id.radio_strength_super_strong -> PasswordStrength.SUPER_STRONG
            R.id.radio_strength_extreme -> PasswordStrength.EXTREME
            else -> PASSWD_STRENGTH_DEFAULT // default
        }
        return passphraseGenerator.generatePassphrase(
            PassphraseGeneratorSpec(
                strength = passwordStrength,
                wordBeginningUpperCase = switchUpperCaseChar.isChecked,
                addDigit = switchAddDigit.isChecked,
                addSpecialChar = switchAddSpecialChar.isChecked))
    }

    private fun buildRadioButton(radioButton: RadioButton, passwordStrength: PasswordStrength) {
        val name = getResources().getString(passwordStrength.nameId)
        radioButton.text = "${name}"
        if (PASSWD_STRENGTH_DEFAULT == passwordStrength) {
            radioButton.setChecked(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(this, MainActivity1::class.java)
            navigateUpTo(upIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}