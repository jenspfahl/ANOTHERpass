package de.jepfa.yapm.ui.editcredential

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.setPadding
import com.google.android.material.tabs.TabLayout
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secretgenerator.*
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.password.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.password.PasswordGeneratorSpec
import de.jepfa.yapm.ui.SecureFragment
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.*
import java.util.*
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

    private var optionsMenu: Menu? = null

    private var generatedPassword: Password = Password.empty()
    private var originCredential: EncCredential? = null

    private val passphraseGenerator = PassphraseGenerator()
    private val passwordGenerator = PasswordGenerator()

    private var passwordCombinations: Double? = null

    private var obfuscatePasswordRequired = false
    private var obfuscationKey: Key? = null

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

        val editPasswdImageView: ImageView = view.findViewById(R.id.imageview_edit_passwd)
        generatedPasswdView = view.findViewById(R.id.generated_passwd)
        radioStrength = view.findViewById(R.id.radio_strengths)
        passwdTypeTab = view.findViewById(R.id.tab_passwd_type)

        var tab = getPreferedTab()
        if (tab != null) passwdTypeTab.selectTab(tab)

        //fill UI
        if (editCredentialActivity.isUpdate()) {
            originCredential = editCredentialActivity.original

            originCredential?.let { _originCredential ->
                masterSecretKey?.let{ key ->
                    val password = SecretService.decryptPassword(key, _originCredential.password)
                    updatePasswordView(password, guessPasswordCombinations = true,
                        _originCredential.isObfuscated)
                }
                updateObfuscationMenuItems(_originCredential)
            }

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
            editCredentialActivity.current.isObfuscated = false
            unsetObfuscation(editCredentialActivity.current)
            updatePasswordView(password, guessPasswordCombinations = false,
                editCredentialActivity.current.isObfuscated)
        }

        editPasswdImageView.setOnClickListener {

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
                    editCredentialActivity.current.isObfuscated = false
                    unsetObfuscation(editCredentialActivity.current)
                    updatePasswordView(password, guessPasswordCombinations = true,
                        editCredentialActivity.current.isObfuscated)
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
                    if (obfuscatePasswordRequired) {
                        val inputView = LinearLayout(context)
                        inputView.orientation = LinearLayout.VERTICAL
                        inputView.setPadding(32)

                        val pwd1 = EditText(context)
                        pwd1.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        val filters =
                            arrayOf<InputFilter>(InputFilter.LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
                        pwd1.filters = filters
                        pwd1.requestFocus()
                        inputView.addView(pwd1)

                        val pwd2 = EditText(context)
                        pwd2.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        pwd2.setFilters(filters)
                        inputView.addView(pwd2)

                        val builder = AlertDialog.Builder(context)
                        val dialog: AlertDialog = builder
                            .setTitle(R.string.obfuscate_while_saving)
                            .setMessage(R.string.obfuscate_while_saving_message)
                            .setView(inputView)
                            .setPositiveButton(android.R.string.ok, null)
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()

                        dialog.setOnShowListener {
                            val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            buttonPositive.setOnClickListener {
                                val obfusPasswd1 = Password.fromEditable(pwd1.text)
                                val obfusPasswd2 = Password.fromEditable(pwd2.text)
                                if (obfusPasswd1.isEmpty()) {
                                    pwd1.setError(getString(R.string.error_field_required))
                                    pwd1.requestFocus()
                                    return@setOnClickListener
                                }
                                else if (obfusPasswd2.isEmpty()) {
                                    pwd2.setError(getString(R.string.error_field_required))
                                    pwd2.requestFocus()
                                    return@setOnClickListener
                                }
                                else if (!obfusPasswd1.isEqual(obfusPasswd2)) {
                                    pwd2.setError(getString(R.string.password_not_equal))
                                    pwd2.requestFocus()
                                    return@setOnClickListener
                                }

                                context?.let {
                                    val salt = SaltService.getSalt(it)
                                    val cipherAlgorithm = SecretService.getCipherAlgorithm(it)
                                    val obfuscationSK =
                                        SecretService.generateNormalSecretKey(obfusPasswd1, salt, cipherAlgorithm)
                                    val obfuscationKey = SecretService.secretKeyToKey(obfuscationSK, salt)
                                    generatedPassword.obfuscate(obfuscationKey)
                                    obfuscationKey.clear()

                                    editCredentialActivity.current.isObfuscated = true
                                    saveCredential(key, editCredentialActivity)
                                }

                                dialog.dismiss()
                            }
                            val buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            buttonNegative.setOnClickListener { dialog.dismiss() }
                        }
                        dialog.show()

                    }
                    else {
                        saveCredential(key, editCredentialActivity)
                    }
                }
            }
        }
    }

    private fun saveCredential(
        key: SecretKeyHolder,
        editCredentialActivity: EditCredentialActivity
    ) {
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
            } else {
                updateCredential(key, saveLastPassword = false, editCredentialActivity)
            }
            originPasswd.clear()
        } else {
            updateCredential(key, saveLastPassword = false, editCredentialActivity)
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

    private fun updatePasswordView(password: Password, guessPasswordCombinations: Boolean, isObfuscated: Boolean) {
        getSecureActivity()?.let {
            generatedPassword = password
            if (!generatedPassword.isEmpty()) {
                var spannedString =
                    PasswordColorizer.spannableObfusableString(
                        generatedPassword,
                        isObfuscated,
                        it)
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
        key: SecretKeyHolder,
        saveLastPassword: Boolean,
        editCredentialActivity: EditCredentialActivity
    ) {
        val encPassword = SecretService.encryptPassword(key, generatedPassword)
        generatedPassword.clear()

        if (saveLastPassword) {
            editCredentialActivity.current.lastPassword = editCredentialActivity.original?.password
            editCredentialActivity.current.isLastPasswordObfuscated = editCredentialActivity.original?.isObfuscated ?: false
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
                        DetachHelper.detachPassword(it, encPassword, null, null)
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
                        ClipboardUtil.copyEncPasswordWithCheck(encPassword, null, it)
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
                val isLastPasswordObfuscated = editCredentialActivity.current.isLastPasswordObfuscated ?: false

                if (lastPasswd == null || lastPasswd.isBlank() || !lastPasswd.isValid()) {
                    Toast.makeText(activity, getString(R.string.nothing_to_restore), Toast.LENGTH_LONG).show()
                }
                else {
                    editCredentialActivity.current.isObfuscated = isLastPasswordObfuscated
                    unsetObfuscation(editCredentialActivity.current)
                    updatePasswordView(lastPasswd, guessPasswordCombinations = true,
                        editCredentialActivity.current.isObfuscated)
                }
            }

            return true
        }

        if (id == R.id.menu_deobfuscate_password) {

            masterSecretKey?.let{ key ->

                if (obfuscationKey != null) {
                    obfuscationKey?.let {
                        generatedPassword.obfuscate(it)
                        editCredentialActivity.current.isObfuscated = true
                        updatePasswordView(generatedPassword, guessPasswordCombinations = true,
                            editCredentialActivity.current.isObfuscated)
                    }

                    item.isChecked = false
                    unsetObfuscation(editCredentialActivity.current)

                    Toast.makeText(context, R.string.deobfuscate_restored, Toast.LENGTH_LONG).show()
                }
                else {
                    context?.let { ctx ->
                        DeobfuscationDialog.openDeobfuscationDialog(ctx) { newObfuscationKey ->
                            item.isChecked = true

                            obfuscationKey = newObfuscationKey
                            obfuscationKey?.let {
                                generatedPassword.deobfuscate(it)
                                editCredentialActivity.current.isObfuscated = false
                                updatePasswordView(generatedPassword, guessPasswordCombinations = true,
                                    editCredentialActivity.current.isObfuscated)
                            }
                            updateObfuscationMenuItems(editCredentialActivity.current)

                            Toast.makeText(ctx, R.string.password_deobfuscated, Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            }
            return true
        }

        if (id == R.id.menu_obfuscate_password) {
            obfuscatePasswordRequired = !obfuscatePasswordRequired
            item.isChecked = obfuscatePasswordRequired

            if (obfuscatePasswordRequired) {
                Toast.makeText(context, R.string.obfuscate_while_saving_toast, Toast.LENGTH_LONG).show()
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.credential_edit_menu, menu)

        optionsMenu = menu

        val enableCopyPassword = PreferenceService.getAsBool(PreferenceService.PREF_ENABLE_COPY_PASSWORD, getBaseActivity())
        if (!enableCopyPassword) {
            menu.findItem(R.id.menu_copy_credential)?.isVisible = false
        }

        val enableOverlayFeature = PreferenceService.getAsBool(PreferenceService.PREF_ENABLE_OVERLAY_FEATURE, getBaseActivity())
        if (!enableOverlayFeature) {
            menu.findItem(R.id.menu_detach_credential)?.isVisible = false
        }

        updateObfuscationMenuItems(originCredential)
    }

    private fun unsetObfuscation(credential: EncCredential?) {
        obfuscationKey?.clear()
        obfuscationKey = null
        updateObfuscationMenuItems(credential)
    }

    private fun updateObfuscationMenuItems(credential: EncCredential?) {
        if (credential == null) {
            optionsMenu?.findItem(R.id.menu_obfuscate_password)?.isVisible = true
            optionsMenu?.findItem(R.id.menu_deobfuscate_password)?.isVisible = false
        }
        credential?.let {
            if (it.isObfuscated) {
                optionsMenu?.findItem(R.id.menu_obfuscate_password)?.isVisible = false
                optionsMenu?.findItem(R.id.menu_deobfuscate_password)?.isVisible = true
            }
            else {
                optionsMenu?.findItem(R.id.menu_obfuscate_password)?.isVisible = true
                optionsMenu?.findItem(R.id.menu_deobfuscate_password)?.isVisible = false
            }
        }
    }


}