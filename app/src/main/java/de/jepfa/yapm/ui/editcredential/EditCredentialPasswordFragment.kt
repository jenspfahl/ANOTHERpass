package de.jepfa.yapm.ui.editcredential

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.tabs.TabLayout
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secretgenerator.*
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.password.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.password.PasswordGeneratorSpec
import de.jepfa.yapm.ui.SecureFragment
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.*
import javax.crypto.SecretKey


class EditCredentialPasswordFragment : SecureFragment() {

    val PASSPHRASE_STRENGTH_DEFAULT = SecretStrength.STRONG
    val PASSWORD_STRENGTH_DEFAULT = SecretStrength.STRONG

    private lateinit var generatedPasswdView: TextView
    private lateinit var passwdTypeTab: TabLayout
    private lateinit var switchUpperCaseChar: SwitchCompat
    private lateinit var switchAddDigit: SwitchCompat
    private lateinit var switchAddSpecialChar: SwitchCompat
    private lateinit var radioStrength: RadioGroup

    private var generatedPassword: Password = Password.empty()
    private var originCredential: EncCredential? = null

    private val passphraseGenerator = PassphraseGenerator()
    private val passwordGenerator = PasswordGenerator()

    private var passwordCombinations: Double? = null

    init {
        enableBack = true
        backToPreviousFragment = true
    }
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        if (Session.isDenied()) {
            getSecureActivity()?.let { LockVaultUseCase.execute(it) }
            return null
        }
        return inflater.inflate(R.layout.fragment_edit_credential_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        val editCredentialActivity = getBaseActivity() as EditCredentialActivity

        generatedPasswdView = view.findViewById(R.id.generated_passwd)
        radioStrength = view.findViewById(R.id.radio_strengths)
        passwdTypeTab = view.findViewById(R.id.tab_passwd_type)

        var tab = getPreferedTab()
        if (tab != null) passwdTypeTab.selectTab(tab)

        //fill UI
        if (editCredentialActivity.isUpdate()) {
            editCredentialActivity.load().observe(editCredentialActivity, {
                originCredential = it
                masterSecretKey?.let{ key ->
                    val password = SecretService.decryptPassword(key, it.password)
                    updatePasswordView(password, guessPasswordCombinations = true)
                }
            })
        }

        val radioStrengthNormal: RadioButton = view.findViewById(R.id.radio_strength_normal)
        val radioStrengthStrong: RadioButton = view.findViewById(R.id.radio_strength_strong)
        val radioStrengthSuperStrong: RadioButton = view.findViewById(R.id.radio_strength_super_strong)
        val radioStrengthExtreme: RadioButton = view.findViewById(R.id.radio_strength_extreme)

        buildRadioButton(radioStrengthNormal, SecretStrength.NORMAL)
        buildRadioButton(radioStrengthStrong, SecretStrength.STRONG)
        buildRadioButton(radioStrengthSuperStrong, SecretStrength.ULTRA)
        buildRadioButton(radioStrengthExtreme, SecretStrength.EXTREME)

        switchUpperCaseChar = view.findViewById(R.id.switch_upper_case_char)
        switchAddDigit = view.findViewById(R.id.switch_add_digit)
        switchAddSpecialChar = view.findViewById(R.id.switch_add_special_char)

        switchUpperCaseChar.isChecked = getPref(PreferenceService.PREF_WITH_UPPER_CASE)
        switchAddDigit.isChecked = getPref(PreferenceService.PREF_WITH_DIGITS)
        switchAddSpecialChar.isChecked = getPref(PreferenceService.PREF_WITH_SPECIAL_CHARS)

        val buttonGeneratePasswd: Button = view.findViewById(R.id.button_generate_passwd)
        buttonGeneratePasswd.setOnClickListener {
            val password = generatePassword()
            updatePasswordView(password, guessPasswordCombinations = false)
        }

        generatedPasswdView.setOnLongClickListener {

            val input = EditText(getBaseActivity())
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.setText(generatedPassword, TextView.BufferType.EDITABLE)

            val filters = arrayOf<InputFilter>(LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
            input.setFilters(filters)

            AlertDialog.Builder(getBaseActivity())
                .setTitle(R.string.edit_password)
                .setMessage(R.string.edit_password_message)
                .setView(input)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    val password = Password(input.text.toString())
                    updatePasswordView(password, guessPasswordCombinations = true)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, which ->
                    dialog.cancel()
                }
                .show()

            true
        }

        generatedPasswdView.setOnClickListener {
            showPasswordStrength()
        }

        val buttonSave: Button = view.findViewById(R.id.button_save)
        buttonSave.setOnClickListener {

            if (generatedPassword.data.isEmpty()) {
                Toast.makeText(it.context, getString(R.string.generate_password_first), Toast.LENGTH_LONG).show()
            }
            else {
                masterSecretKey?.let{ key ->
                    val origCredential = originCredential
                    if (origCredential != null) {
                        val originPasswd =
                            SecretService.decryptPassword(key, origCredential.password)
                        if (editCredentialActivity.current.isPersistent()
                            && !generatedPassword.isEqual(originPasswd)
                        ) {
                            AlertDialog.Builder(editCredentialActivity)
                                .setTitle(R.string.title_change_credential)
                                .setMessage(R.string.message_password_changed)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(R.string.title_continue) { dialog, whichButton ->
                                    updateCredential(key, saveLastPassword = true, editCredentialActivity)
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        }
                        else {
                            updateCredential(key, saveLastPassword = false, editCredentialActivity)
                        }
                        originPasswd.clear()
                    }
                    else {
                        updateCredential(key, saveLastPassword = false, editCredentialActivity)
                    }
                }
            }
        }
    }

    private fun guessPasswordCombinations(password: Password) {
        // rudimentary combination calculation by assuming a-Z, A-Z, 0-9 and 10 potential special chars
        val containsLowerCase = if (password.toString().contains(Regex("[a-z]"))) 26 else 0
        val containsUpperCase = if (password.toString().contains(Regex("[A-Z]"))) 26 else 0
        val containsDigits = if (password.toString().contains(Regex("[0-9]"))) 10 else 0
        passwordCombinations = Math.pow(
            (containsLowerCase + containsUpperCase + containsDigits + 10).toDouble(),
            password.length.toDouble()
        )
    }

    private fun calcPasswordStrength() {
        passwordCombinations = if (isPassphraseSelected()) {
            passphraseGenerator.calcCombinationCount(buildPassphraseGeneratorSpec())
        } else {
            passwordGenerator.calcCombinationCount(buildPasswordGeneratorSpec())
        }

    }

    private fun showPasswordStrength() {
        val combinations = passwordCombinations
        if (combinations != null) {
            val bruteForceWithPentium = passphraseGenerator.calcBruteForceWaitingSeconds(
                combinations, GeneratorBase.BRUTEFORCE_ATTEMPTS_PENTIUM
            )
            val bruteForceWithSupercomp = passphraseGenerator.calcBruteForceWaitingSeconds(
                combinations, GeneratorBase.BRUTEFORCE_ATTEMPTS_SUPERCOMP
            )

            AlertDialog.Builder(context)
                .setTitle(getString(R.string.password_strength))
                .setMessage(
                    getString(R.string.combinations) + ": " +
                            System.lineSeparator() +
                            combinations.toReadableFormat(0) +
                            System.lineSeparator() +
                            "($combinations)" +
                            System.lineSeparator() +
                            System.lineSeparator() +
                            getString(R.string.bruteforce_years_pc) + ": " +
                            System.lineSeparator() +
                            bruteForceWithPentium.secondsToYear().toReadableFormat(0) +
                            System.lineSeparator() +
                            "(${bruteForceWithPentium.secondsToYear()})" +
                            System.lineSeparator() +
                            System.lineSeparator() +
                            getString(R.string.bruteforce_year_supercomp) + ": " +
                            System.lineSeparator() +
                            bruteForceWithSupercomp.secondsToYear().toReadableFormat(0) +
                            System.lineSeparator() +
                            "(${bruteForceWithSupercomp.secondsToYear()})"
                )
                .show()
        }
    }

    private fun updatePasswordView(password: Password, guessPasswordCombinations: Boolean) {
        getSecureActivity()?.let {
            generatedPassword = password
            if (!generatedPassword.isEmpty()) {
                var spannedString =
                    PasswordColorizer.spannableString(generatedPassword, it)
                generatedPasswdView.text = spannedString
                if (guessPasswordCombinations) {
                    guessPasswordCombinations(password)
                }
                else {
                    calcPasswordStrength()
                }
            }
            else {
                generatedPasswdView.text = getString(R.string.nothing_placeholder)
                passwordCombinations = null
            }
        }
    }

    private fun updateCredential(
        key: SecretKey,
        saveLastPassword: Boolean,
        editCredentialActivity: EditCredentialActivity
    ) {
        val encPassword = SecretService.encryptPassword(key, generatedPassword)
        generatedPassword.clear()

        if (saveLastPassword) {
            editCredentialActivity.current.lastPassword = editCredentialActivity.original?.password
        }
        editCredentialActivity.current.password = encPassword

        editCredentialActivity.reply()
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
        if (getPref(PreferenceService.PREF_USE_PREUDO_PHRASE)) {
            return passwdTypeTab.getTabAt(0)
        }
        else {
            return passwdTypeTab.getTabAt(1)
        }
    }

    private fun buildPassphraseGeneratorSpec(): PassphraseGeneratorSpec {
        val passphraseStrength = when (radioStrength.checkedRadioButtonId) {
            R.id.radio_strength_normal -> SecretStrength.NORMAL
            R.id.radio_strength_strong -> SecretStrength.STRONG
            R.id.radio_strength_super_strong -> SecretStrength.ULTRA
            R.id.radio_strength_extreme -> SecretStrength.EXTREME
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
            R.id.radio_strength_normal -> SecretStrength.NORMAL
            R.id.radio_strength_strong -> SecretStrength.STRONG
            R.id.radio_strength_super_strong -> SecretStrength.ULTRA
            R.id.radio_strength_extreme -> SecretStrength.EXTREME
            else -> PASSWORD_STRENGTH_DEFAULT // default
        }
        val spec = PasswordGeneratorSpec(
            strength = passwordStrength,
            onlyLowerCase = !switchUpperCaseChar.isChecked,
            noDigits = !switchAddDigit.isChecked,
            excludeSpecialChars = !switchAddSpecialChar.isChecked)
        return spec
    }

    private fun buildRadioButton(radioButton: RadioButton, passphraseStrength: SecretStrength) {
        val name = resources.getString(passphraseStrength.nameId)
        radioButton.text = name

        val prefDefaultPasswdStrength = PreferenceService.getAsString(PreferenceService.PREF_PASSWD_STRENGTH, getBaseActivity())
        val defaultPasswdStrength = getStrengthEnum(prefDefaultPasswdStrength)
        if (defaultPasswdStrength == passphraseStrength) {
            radioButton.isChecked = true
        }
    }

    private fun getPref(key: String): Boolean {
        return PreferenceService.getAsBool(key, getBaseActivity())
    }

    private fun getStrengthEnum(name: String?) : SecretStrength {
        name ?: return PASSPHRASE_STRENGTH_DEFAULT
        return SecretStrength.valueOf(name)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val editCredentialActivity = getBaseActivity() as EditCredentialActivity

        val id = item.itemId
        if (id == R.id.menu_detach_credential) {

            masterSecretKey?.let{ key ->
                if (generatedPassword.data.isEmpty()) {
                    Toast.makeText(activity, getString(R.string.generate_password_first), Toast.LENGTH_LONG).show()
                }
                else {
                    getSecureActivity()?.let {
                        val encPassword = SecretService.encryptPassword(key, generatedPassword)
                        DetachHelper.detachPassword(it, encPassword, null)
                    }
                }
            }

            return true
        }

        if (id == R.id.menu_copy_credential) {
            masterSecretKey?.let{ key ->
                if (generatedPassword.data.isEmpty()) {
                    Toast.makeText(activity, getString(R.string.generate_password_first), Toast.LENGTH_LONG).show()
                }
                else {
                    getSecureActivity()?.let {
                        val encPassword = SecretService.encryptPassword(key, generatedPassword)
                        ClipboardUtil.copyEncPasswordWithCheck(encPassword, it)
                    }
                }
            }
            return true
        }

        if (id == R.id.menu_restore_last_password) {

            masterSecretKey?.let{ key ->

                val lastPasswd = editCredentialActivity.current.lastPassword?.let {
                    SecretService.decryptPassword(key, it)
                }

                if (lastPasswd == null || lastPasswd.isBlank() || !lastPasswd.isValid()) {
                    Toast.makeText(activity, getString(R.string.nothing_to_restore), Toast.LENGTH_LONG).show()
                }
                else {
                    updatePasswordView(lastPasswd, guessPasswordCombinations = true)
                }
            }

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.credential_edit_menu, menu)

        val enableCopyPassword = PreferenceService.getAsBool(PreferenceService.PREF_ENABLE_COPY_PASSWORD, getBaseActivity())
        if (!enableCopyPassword) {
            menu.findItem(R.id.menu_copy_credential)?.isVisible = false
        }

        val enableOverlayFeature = PreferenceService.getAsBool(PreferenceService.PREF_ENABLE_OVERLAY_FEATURE, getBaseActivity())
        if (!enableOverlayFeature) {
            menu.findItem(R.id.menu_detach_credential)?.isVisible = false
        }

    }


}