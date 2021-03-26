package de.jepfa.yapm.ui.credential

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import com.google.android.material.tabs.TabLayout
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secretgenerator.*
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_PENTIUM
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_SUPERCOMP
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.PREF_USE_PREUDO_PHRASE
import de.jepfa.yapm.util.PreferenceUtil.PREF_WITH_DIGITS
import de.jepfa.yapm.util.PreferenceUtil.PREF_WITH_SPECIAL_CHARS
import de.jepfa.yapm.util.PreferenceUtil.PREF_WITH_UPPER_CASE
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory


class NewOrChangeCredentialActivity : SecureActivity() {

    val PASSPHRASE_STRENGTH_DEFAULT = PassphraseStrength.STRONG
    val PASSWORD_STRENGTH_DEFAULT = PasswordStrength.STRONG

    private lateinit var editCredentialNameView: EditText
    private lateinit var editCredentialAdditionalInfoView: EditText
    private lateinit var generatedPasswdView: TextView
    private lateinit var passwdTypeTab: TabLayout
    private lateinit var switchUpperCaseChar: Switch
    private lateinit var switchAddDigit: Switch
    private lateinit var switchAddSpecialChar: Switch
    private lateinit var radioStrength: RadioGroup
    private lateinit var buttonSave: Button

    private var generatedPassword: Password = Password.empty()
    private var currentId: Int = -1

    private val passphraseGenerator = PassphraseGenerator()
    private val passwordGenerator = PasswordGenerator()

    private val credentialViewModel: CredentialViewModel by viewModels {
        CredentialViewModelFactory((application as YapmApp).repository)
    }

    @SuppressLint("ResourceType")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            return
        }
        setContentView(R.layout.activity_new_or_change_credential)
        editCredentialNameView = findViewById(R.id.edit_credential_name)
        editCredentialAdditionalInfoView = findViewById(R.id.edit_credential_additional_info)
        generatedPasswdView = findViewById(R.id.generated_passwd)
        radioStrength = findViewById(R.id.radio_strengths)
        passwdTypeTab = findViewById(R.id.tab_passwd_type)

        var tab = getPreferedTab()
        if (tab != null) passwdTypeTab.selectTab(tab)

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

        val radioStrengthNormal: RadioButton = findViewById(R.id.radio_strength_normal)
        val radioStrengthStrong: RadioButton = findViewById(R.id.radio_strength_strong)
        val radioStrengthSuperStrong: RadioButton = findViewById(R.id.radio_strength_super_strong)
        val radioStrengthExtreme: RadioButton = findViewById(R.id.radio_strength_extreme)

        buildRadioButton(radioStrengthNormal, PassphraseStrength.NORMAL)
        buildRadioButton(radioStrengthStrong, PassphraseStrength.STRONG)
        buildRadioButton(radioStrengthSuperStrong, PassphraseStrength.SUPER_STRONG)
        buildRadioButton(radioStrengthExtreme, PassphraseStrength.EXTREME)

        switchUpperCaseChar = findViewById(R.id.switch_upper_case_char)
        switchAddDigit = findViewById(R.id.switch_add_digit)
        switchAddSpecialChar = findViewById(R.id.switch_add_special_char)

        switchUpperCaseChar.isChecked = getPref(PREF_WITH_UPPER_CASE, true)
        switchAddDigit.isChecked = getPref(PREF_WITH_DIGITS, false)
        switchAddSpecialChar.isChecked = getPref(PREF_WITH_SPECIAL_CHARS, false)

        val buttonGeneratePasswd: Button = findViewById(R.id.button_generate_passwd)
        buttonGeneratePasswd.setOnClickListener(View.OnClickListener {
            Session.safeTouch()
            generatedPassword = generatePassword()
            generatedPasswdView.text = generatedPassword.debugToString()
        })

        generatedPasswdView.setOnClickListener {

            val combinations = if (isPassphraseSelected()) {
                passphraseGenerator.calcCombinationCount(buildPassphraseGeneratorSpec())
            }
            else {
                passwordGenerator.calcCombinationCount(buildPasswordGeneratorSpec())
            }

            val bruteForceWithPentum = passphraseGenerator.calcBruteForceWaitingSeconds(
                    combinations, BRUTEFORCE_ATTEMPTS_PENTIUM)
            val bruteForceWithSupercomp = passphraseGenerator.calcBruteForceWaitingSeconds(
                    combinations, BRUTEFORCE_ATTEMPTS_SUPERCOMP)
            AlertDialog.Builder(it.context)
                    .setTitle("Password strength")
                    .setMessage("Combinations: $combinations" + System.lineSeparator() + System.lineSeparator() +
                            "Years to brute force with a usual PC: ${bruteForceWithPentum / 60 / 60 / 24 / 365}" + System.lineSeparator() + System.lineSeparator() +
                            "Years to brute force with a super computer: ${bruteForceWithSupercomp / 60 / 60 / 24 / 365}")
                    .show()
        }

        buttonSave = findViewById(R.id.button_save)
        buttonSave.setOnClickListener {
            Session.safeTouch()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.credential_new_or_update_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    private fun generatePassword() : Password {

        if (isPassphraseSelected()) {
            val spec = buildPassphraseGeneratorSpec()
            return passphraseGenerator.generate(spec)
        }
        else {
            val spec = buildPasswordGeneratorSpec()
            return passwordGenerator.generate(spec)
        }

    }

    private fun isPassphraseSelected() : Boolean {
        return passwdTypeTab.selectedTabPosition == 0
    }

    private fun getPreferedTab(): TabLayout.Tab? {
        if (getPref(PREF_USE_PREUDO_PHRASE, true)) {
            return passwdTypeTab.getTabAt(0)
        }
        else {
            return passwdTypeTab.getTabAt(1)
        }
    }

    private fun buildPassphraseGeneratorSpec(): PassphraseGeneratorSpec {
        val passphraseStrength = when (radioStrength.checkedRadioButtonId) {
            R.id.radio_strength_normal -> PassphraseStrength.NORMAL
            R.id.radio_strength_strong -> PassphraseStrength.STRONG
            R.id.radio_strength_super_strong -> PassphraseStrength.SUPER_STRONG
            R.id.radio_strength_extreme -> PassphraseStrength.EXTREME
            else -> PASSPHRASE_STRENGTH_DEFAULT // default
        }
        val spec = PassphraseGeneratorSpec(
                strength = passphraseStrength,
                wordBeginningUpperCase = switchUpperCaseChar.isChecked,
                addDigit = switchAddDigit.isChecked,
                addSpecialChar = switchAddSpecialChar.isChecked)
        return spec
    }

    private fun buildPasswordGeneratorSpec(): PasswordGeneratorSpec {
        val passwordStrength = when (radioStrength.checkedRadioButtonId) {
            R.id.radio_strength_normal -> PasswordStrength.NORMAL
            R.id.radio_strength_strong -> PasswordStrength.STRONG
            R.id.radio_strength_super_strong -> PasswordStrength.SUPER_STRONG
            R.id.radio_strength_extreme -> PasswordStrength.EXTREME
            else -> PASSWORD_STRENGTH_DEFAULT // default
        }
        val spec = PasswordGeneratorSpec(
                strength = passwordStrength,
                onlyLowerCase = !switchUpperCaseChar.isChecked,
                noDigits = !switchAddDigit.isChecked,
                excludeSpecialChars = !switchAddSpecialChar.isChecked)
        return spec
    }

    private fun buildRadioButton(radioButton: RadioButton, passphraseStrength: PassphraseStrength) {
        val name = getResources().getString(passphraseStrength.nameId)
        radioButton.text = "${name}"
        if (PASSPHRASE_STRENGTH_DEFAULT == passphraseStrength) {
            radioButton.setChecked(true)
        }
    }

    private fun getPref(key: String, default: Boolean): Boolean {
        return PreferenceUtil.getBool(key, default, this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(this, ListCredentialsActivity::class.java)
            navigateUpTo(upIntent)
            return true
        }

        if (id == R.id.menu_detach_credential) {

            val key = masterSecretKey
            if (key != null) {
                if (generatedPassword.data.isEmpty()) {
                    Toast.makeText(this, "Generate a password first", Toast.LENGTH_LONG).show()
                }
                else {
                    val encPassword = SecretService.encryptPassword(key, generatedPassword)

                    DetachHelper.detachPassword(this, encPassword)
                }
            }

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun lock() {
        generatedPassword.clear()
        recreate()
    }

}