package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.service.secretgenerator.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory

class NewOrChangeCredentialActivity : SecureActivity() {

    private lateinit var editCredentialNameView: EditText
    private lateinit var editCredentialAdditionalInfoView: EditText
    private lateinit var generatedPasswdView: TextView
    private lateinit var switchUpperCaseChar: Switch
    private lateinit var switchAddDigit: Switch
    private lateinit var switchAddSpecialChar: Switch
    private lateinit var radioStrength: RadioGroup

    private var generatedPassword: Password = Password("")
    private var currentId: Int = -1

    private val passphraseGenerator = PassphraseGenerator()

    private val credentialViewModel: CredentialViewModel by viewModels {
        CredentialViewModelFactory((application as YapmApp).repository)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_or_change_credential)
        editCredentialNameView = findViewById(R.id.edit_credential_name)
        editCredentialAdditionalInfoView = findViewById(R.id.edit_credential_additional_info)
        generatedPasswdView = findViewById(R.id.generated_passwd)
        radioStrength = findViewById(R.id.radio_strengths)


        val idExtra = intent.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)


        if (idExtra == -1) {
            setTitle(R.string.title_new_credential)
        }
        else {
            currentId = idExtra
            setTitle(R.string.title_update_credential)

            credentialViewModel.getById(idExtra).observe(this, {
                val originCredential = it
                val key = masterSecretKey
                if (key != null) {
                    val name = SecretService.decryptCommonString(key, originCredential.name)
                    val additionalInfo = SecretService.decryptCommonString(key, originCredential.additionalInfo)
                    val password = SecretService.decryptPassword(key, originCredential.password)
                    editCredentialNameView.setText(name)
                    editCredentialAdditionalInfoView.setText(additionalInfo)
                    generatedPassword = password
                    generatedPasswdView.setText(password.debugToString())
                }
            })
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
            Secret.touch()
            generatedPassword = generatePassword()
            generatedPasswdView.text = generatedPassword.debugToString()
        })

        generatedPasswdView.setOnClickListener {
            val spec = buildGeneratorSpec()
            val combinations = passphraseGenerator.calcCombinationCount(spec)
            val bruteForceWithPentum = passphraseGenerator.calcBruteForceWaitingSeconds(
                    spec, passphraseGenerator.BRUTEFORCE_ATTEMPTS_PENTIUM)
            val bruteForceWithSupercomp = passphraseGenerator.calcBruteForceWaitingSeconds(
                    spec, passphraseGenerator.BRUTEFORCE_ATTEMPTS_SUPERCOMP)
            AlertDialog.Builder(it.context)
                    .setTitle("Password strength")
                    .setMessage("Combinations: $combinations" + System.lineSeparator() + System.lineSeparator() +
                            "Years to brute force with a usual PC: ${bruteForceWithPentum/60/60/24/365}" + System.lineSeparator() + System.lineSeparator() +
                            "Years to brute force with a super computer: ${bruteForceWithSupercomp/60/60/24/365}")
                    .show()
        }

        val button = findViewById<Button>(R.id.button_save)
        button.setOnClickListener {
            Secret.touch()
            if (generatedPassword.data.isEmpty()) {
                Toast.makeText(it.context, "Generate a password first", Toast.LENGTH_LONG).show()
            }
            else {
                val replyIntent = Intent()
                if (TextUtils.isEmpty(editCredentialNameView.text)) {
                    editCredentialNameView.setError(getString(R.string.error_field_required))
                    editCredentialNameView.requestFocus()
                } else {
                    val key = masterSecretKey
                    if (key != null) {
                        val name = editCredentialNameView.text.toString()
                        val additionalInfo = editCredentialAdditionalInfoView.text.toString()

                        val encName = SecretService.encryptCommonString(key, name)
                        val encAdditionalInfo = SecretService.encryptCommonString(key, additionalInfo)
                        val encPassword = SecretService.encryptPassword(key, generatedPassword)
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
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun generatePassword() : Password {
        val spec = buildGeneratorSpec()

        return passphraseGenerator.generatePassphrase(spec)
    }

    private fun buildGeneratorSpec(): PassphraseGeneratorSpec {
        val passwordStrength = when (radioStrength.checkedRadioButtonId) {
            R.id.radio_strength_strong -> PasswordStrength.STRONG
            R.id.radio_strength_super_strong -> PasswordStrength.SUPER_STRONG
            R.id.radio_strength_extreme -> PasswordStrength.EXTREME
            else -> PASSWD_STRENGTH_DEFAULT // default
        }
        val spec = PassphraseGeneratorSpec(
                strength = passwordStrength,
                wordBeginningUpperCase = switchUpperCaseChar.isChecked,
                addDigit = switchAddDigit.isChecked,
                addSpecialChar = switchAddSpecialChar.isChecked)
        return spec
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
            val upIntent = Intent(this, ListCredentialsActivity::class.java)
            navigateUpTo(upIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun refresh(before: Boolean) {
        //TODO
        if (!before) {
            recreate()
        }
    }

}