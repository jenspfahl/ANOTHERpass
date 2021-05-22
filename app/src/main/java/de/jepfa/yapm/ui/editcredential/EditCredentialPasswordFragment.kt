package de.jepfa.yapm.ui.editcredential

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import com.google.android.material.tabs.TabLayout
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secretgenerator.*
import de.jepfa.yapm.ui.SecureFragment
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.*


class EditCredentialPasswordFragment : SecureFragment() {

    val PASSPHRASE_STRENGTH_DEFAULT = PassphraseStrength.STRONG
    val PASSWORD_STRENGTH_DEFAULT = PasswordStrength.STRONG

    private lateinit var generatedPasswdView: TextView
    private lateinit var passwdTypeTab: TabLayout
    private lateinit var switchUpperCaseChar: Switch
    private lateinit var switchAddDigit: Switch
    private lateinit var switchAddSpecialChar: Switch
    private lateinit var radioStrength: RadioGroup

    private var generatedPassword: Password = Password.empty()

    private val passphraseGenerator = PassphraseGenerator()
    private val passwordGenerator = PasswordGenerator()

    init {
        enableBack = true
        backToPreviousFragment = true
    }
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        if (Session.isDenied()) {
            LockVaultUseCase.execute(getSecureActivity())
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
            editCredentialActivity.load().observe(getSecureActivity(), {
                val originCredential = it
                val key = masterSecretKey
                if (key != null) {
                    val password = SecretService.decryptPassword(key, originCredential.password)
                    generatedPassword = password
                    var spannedString = PasswordColorizer.spannableString(generatedPassword, getSecureActivity())
                    generatedPasswdView.setText(spannedString)
                }
            })
        }

        val radioStrengthNormal: RadioButton = view.findViewById(R.id.radio_strength_normal)
        val radioStrengthStrong: RadioButton = view.findViewById(R.id.radio_strength_strong)
        val radioStrengthSuperStrong: RadioButton = view.findViewById(R.id.radio_strength_super_strong)
        val radioStrengthExtreme: RadioButton = view.findViewById(R.id.radio_strength_extreme)

        buildRadioButton(radioStrengthNormal, PassphraseStrength.NORMAL)
        buildRadioButton(radioStrengthStrong, PassphraseStrength.STRONG)
        buildRadioButton(radioStrengthSuperStrong, PassphraseStrength.SUPER_STRONG)
        buildRadioButton(radioStrengthExtreme, PassphraseStrength.EXTREME)

        switchUpperCaseChar = view.findViewById(R.id.switch_upper_case_char)
        switchAddDigit = view.findViewById(R.id.switch_add_digit)
        switchAddSpecialChar = view.findViewById(R.id.switch_add_special_char)

        switchUpperCaseChar.isChecked = getPref(PreferenceUtil.PREF_WITH_UPPER_CASE, true)
        switchAddDigit.isChecked = getPref(PreferenceUtil.PREF_WITH_DIGITS, false)
        switchAddSpecialChar.isChecked = getPref(PreferenceUtil.PREF_WITH_SPECIAL_CHARS, false)

        val buttonGeneratePasswd: Button = view.findViewById(R.id.button_generate_passwd)
        buttonGeneratePasswd.setOnClickListener {
            generatedPassword = generatePassword()
            var spannedString = PasswordColorizer.spannableString(generatedPassword, getSecureActivity())
            generatedPasswdView.setText(spannedString)
        }

        generatedPasswdView.setOnClickListener {

            val combinations = if (isPassphraseSelected()) {
                passphraseGenerator.calcCombinationCount(buildPassphraseGeneratorSpec())
            }
            else {
                passwordGenerator.calcCombinationCount(buildPasswordGeneratorSpec())
            }

            val bruteForceWithPentium = passphraseGenerator.calcBruteForceWaitingSeconds(
                combinations, GeneratorBase.BRUTEFORCE_ATTEMPTS_PENTIUM
            )
            val bruteForceWithSupercomp = passphraseGenerator.calcBruteForceWaitingSeconds(
                combinations, GeneratorBase.BRUTEFORCE_ATTEMPTS_SUPERCOMP
            )
            AlertDialog.Builder(it.context)
                .setTitle("Password strength")
                .setMessage("Combinations: " +
                        System.lineSeparator() +
                        "${combinations.toReadableFormat(0)}" +
                        System.lineSeparator() +
                        "($combinations)" +
                        System.lineSeparator() +
                        System.lineSeparator() +
                        "Years to brute force with a usual PC: " +
                        System.lineSeparator() +
                        "${bruteForceWithPentium.secondsToYear().toReadableFormat(0)}" +
                        System.lineSeparator() +
                        "(${bruteForceWithPentium.secondsToYear()})" +
                        System.lineSeparator() +
                        System.lineSeparator() +
                        "Years to brute force with a super computer: " +
                        System.lineSeparator() +
                        "${bruteForceWithSupercomp.secondsToYear().toReadableFormat(0)}" +
                        System.lineSeparator() +
                        "(${bruteForceWithSupercomp.secondsToYear()})")
                .show()
        }

        val buttonSave: Button = view.findViewById(R.id.button_save)
        buttonSave.setOnClickListener {

            if (generatedPassword.data.isEmpty()) {
                Toast.makeText(it.context, "Generate a password first", Toast.LENGTH_LONG).show()
            }
            else {
                val key = masterSecretKey
                if (key != null) {

                    val encPassword = SecretService.encryptPassword(key, generatedPassword)
                    generatedPassword.clear()

                    val credentialToSave = editCredentialActivity.current
                    credentialToSave.password = encPassword

                    editCredentialActivity.reply()
                }
            }
        }
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
        if (getPref(PreferenceUtil.PREF_USE_PREUDO_PHRASE, true)) {
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

        val prefDefaultPasswdStrength = PreferenceUtil.get(PreferenceUtil.PREF_PASSWD_STRENGTH, getSecureActivity())
        val defaultPasswdStrength = getStrengthEnum(prefDefaultPasswdStrength)
        if (defaultPasswdStrength == passphraseStrength) {
            radioButton.setChecked(true)
        }
    }

    private fun getPref(key: String, default: Boolean): Boolean {
        return PreferenceUtil.getAsBool(key, default, getSecureActivity())
    }

    private fun getStrengthEnum(name: String?) : PassphraseStrength {
        name ?: return PASSPHRASE_STRENGTH_DEFAULT
        return PassphraseStrength.valueOf(name)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_detach_credential) {

            val key = masterSecretKey
            if (key != null) {
                if (generatedPassword.data.isEmpty()) {
                    Toast.makeText(activity, "Generate a password first", Toast.LENGTH_LONG).show()
                }
                else {
                    val encPassword = SecretService.encryptPassword(key, generatedPassword)
                    DetachHelper.detachPassword(getSecureActivity() , encPassword, null)
                }
            }

            return true
        }

        if (id == R.id.menu_copy_credential) {
            val key = masterSecretKey
            if (key != null) {
                if (generatedPassword.data.isEmpty()) {
                    Toast.makeText(activity, "Generate a password first", Toast.LENGTH_LONG).show()
                }
                else {
                    val encPassword = SecretService.encryptPassword(key, generatedPassword)
                    ClipboardUtil.copyEncPasswordWithCheck(encPassword, getSecureActivity())
                }
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.credential_new_or_update_menu, menu)

        val enableCopyPassword = PreferenceUtil.getAsBool(PreferenceUtil.PREF_ENABLE_COPY_PASSWORD, false, getBaseActivity())
        if (!enableCopyPassword) {
            menu.findItem(R.id.menu_copy_credential)?.isVisible = false
        }

    }


}