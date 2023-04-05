package de.jepfa.yapm.ui.editcredential

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.tabs.TabLayout
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_USE_EXTENDED_SPECIAL_CHARS
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.DEFAULT_OBFUSCATIONABLE_SPECIAL_CHARS
import de.jepfa.yapm.service.secretgenerator.SecretStrength
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.password.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.password.PasswordGeneratorSpec
import de.jepfa.yapm.ui.SecureFragment
import de.jepfa.yapm.ui.credential.DeobfuscationDialog
import de.jepfa.yapm.usecase.credential.ShowPasswordStrengthUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.toastText


class EditCredentialPasswordFragment : SecureFragment() {

    private val PASSPHRASE_STRENGTH_DEFAULT = SecretStrength.STRONG
    private val PASSWORD_STRENGTH_DEFAULT = SecretStrength.STRONG

    private lateinit var currentCredential: EncCredential
    private lateinit var editCredentialActivity: EditCredentialActivity
    private lateinit var generatedPasswdView: TextView
    private lateinit var passwdTypeTab: TabLayout
    private lateinit var switchUpperCaseChar: SwitchCompat
    private lateinit var switchAddDigit: SwitchCompat
    private lateinit var switchAddSpecialChar: SwitchCompat
    private lateinit var radioStrength: RadioGroup

    private var optionsMenu: Menu? = null

    private var generatedPassword: Password = Password.empty()

    private lateinit var passphraseGenerator: PassphraseGenerator
    private lateinit var passwordGenerator: PasswordGenerator

    private var passwordPresentation = Password.FormattingStyle.DEFAULT
    private var multiLine = false

    private var passwordCombinations: Double? = null
    private var passwordCombinationsGuessed = false

    private var obfuscatePasswordRequired = false
    private var obfuscationKey: Key? = null

    init {
        enableBack = true
        backToPreviousFragment = true
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        passphraseGenerator = PassphraseGenerator(context = context)
        passwordGenerator = PasswordGenerator(context = context)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        if (Session.isDenied()) {
            getSecureActivity()?.let { LockVaultUseCase.execute(it) }
            return null
        }

        savedInstanceState?.getBoolean("obfuscatePasswordRequired")?.let {
            obfuscatePasswordRequired = it
        }

        editCredentialActivity = getBaseActivity() as EditCredentialActivity

        val formatted = PreferenceService.getAsBool(PreferenceService.PREF_PASSWD_SHOW_FORMATTED, context)
        multiLine = PreferenceService.getAsBool(PreferenceService.PREF_PASSWD_WORDS_ON_NL, context)
        passwordPresentation = Password.FormattingStyle.createFromFlags(multiLine, formatted)

        return inflater.inflate(R.layout.fragment_edit_credential_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        val editPasswdImageView: ImageView = view.findViewById(R.id.imageview_edit_passwd)
        val passwdStrengthImageView: ImageView = view.findViewById(R.id.imageview_passwd_strength)
        generatedPasswdView = view.findViewById(R.id.generated_passwd)
        radioStrength = view.findViewById(R.id.radio_strengths)
        passwdTypeTab = view.findViewById(R.id.tab_passwd_type)

        val tab = getPreferedTab()
        if (tab != null) passwdTypeTab.selectTab(tab)

        //fill UI
        currentCredential = editCredentialActivity.current?: return

        masterSecretKey?.let{ key ->
            updatePasswordView(key, currentCredential)
        }
        updateObfuscationMenuItems(currentCredential)

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
            currentCredential.isObfuscated = false
            unsetObfuscation(currentCredential)
            updatePasswordView(password, guessPasswordCombinations = false,
                currentCredential.isObfuscated)
        }

        editPasswdImageView.setOnClickListener {

            val input = EditText(getBaseActivity())
            input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            input.setText(generatedPassword.toRawFormattedPassword(), TextView.BufferType.EDITABLE)

            val filters = arrayOf<InputFilter>(LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
            input.setFilters(filters)

            AlertDialog.Builder(editCredentialActivity)
                .setTitle(R.string.edit_password)
                .setMessage(R.string.edit_password_message)
                .setIcon(R.drawable.ic_baseline_edit_24)
                .setView(input)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    val password = Password(input.text)
                    currentCredential.isObfuscated = false
                    unsetObfuscation(currentCredential)
                    updatePasswordView(password, guessPasswordCombinations = true,
                        currentCredential.isObfuscated)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, which ->
                    dialog.cancel()
                }
                .show()

            true
        }

        passwdStrengthImageView.setOnClickListener {
            showPasswordStrength()
        }

        generatedPasswdView.setOnClickListener {
            if (generatedPassword.data.isNotEmpty()) {
                passwordPresentation =
                    if (multiLine) passwordPresentation.prev()
                    else passwordPresentation.next()
                refreshPasswordView(currentCredential)
            }
        }

        val buttonSave: Button = view.findViewById(R.id.button_save)
        buttonSave.setOnClickListener {

            if (generatedPassword.isEmpty()) {
                toastText(it.context, R.string.generate_password_first)
            }
            else {
                masterSecretKey?.let{ key ->
                    if (obfuscatePasswordRequired) {
                        DeobfuscationDialog.openObfuscationDialog(editCredentialActivity,
                            editCredentialActivity.getString(R.string.obfuscate_while_saving),
                            editCredentialActivity.getString(R.string.obfuscate_while_saving_message, DEFAULT_OBFUSCATIONABLE_SPECIAL_CHARS),
                            editCredentialActivity.getString(android.R.string.ok),
                            editCredentialActivity.getString(android.R.string.cancel))
                        { newObfuscationKey ->
                            if (newObfuscationKey != null) {
                                generatedPassword.obfuscate(newObfuscationKey)
                                obfuscationKey = newObfuscationKey

                                currentCredential.isObfuscated = true
                                saveCredential(key, currentCredential)
                            }
                        }
                    }
                    else {
                        saveCredential(key, currentCredential)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("obfuscatePasswordRequired", obfuscatePasswordRequired)
    }

    private fun updatePasswordView(
        key: SecretKeyHolder,
        _originCredential: EncCredential
    ) {
        val password = if (!currentCredential.password.isEmpty()) {
            SecretService.decryptPassword(key, currentCredential.password)
        } else {
            SecretService.decryptPassword(key, _originCredential.password)
        }
        updatePasswordView(
            password, guessPasswordCombinations = true,
            _originCredential.isObfuscated
        )
    }

    private fun saveCredential(
        key: SecretKeyHolder,
        credential: EncCredential
    ) {
        val origCredential = editCredentialActivity.original
        if (origCredential != null) {
            val originPasswd =
                SecretService.decryptPassword(key, origCredential.password)
            if (credential.isPersistent()
                && !generatedPassword.isEqual(originPasswd)
            ) {
                AlertDialog.Builder(editCredentialActivity)
                    .setTitle(R.string.title_change_credential)
                    .setMessage(R.string.message_password_changed)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.yes_continue) { dialog, whichButton ->
                        updateCredential(key, credential, saveLastPassword = true)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                updateCredential(key, credential, saveLastPassword = false)
            }
            originPasswd.clear()
        } else {
            updateCredential(key, credential, saveLastPassword = false)
        }
    }

    private fun guessPasswordCombinations(password: Password) {

        passwordCombinations = ShowPasswordStrengthUseCase.guessPasswordCombinations(password)
        passwordCombinationsGuessed = true
    }

    private fun calcPasswordStrength() {
        passwordCombinations = if (isPassphraseSelected()) {
            passphraseGenerator.calcCombinationCount(buildPassphraseGeneratorSpec())
        } else {
            passwordGenerator.calcCombinationCount(buildPasswordGeneratorSpec())
        }
        passwordCombinationsGuessed = false
    }

    private fun showPasswordStrength() {
        val combinations = passwordCombinations
        if (combinations != null) {

            val titleId =
                if (passwordCombinationsGuessed) R.string.password_strength_guessed
                else R.string.password_strength_from_generator
            ShowPasswordStrengthUseCase.showPasswordStrength(combinations, titleId, editCredentialActivity)
        }
        else {
            toastText(activity,R.string.generate_password_first)
        }
    }

    private fun refreshPasswordView(credential: EncCredential) {
        updatePasswordView(generatedPassword, guessPasswordCombinations = passwordCombinationsGuessed,
            credential.isObfuscated)
    }

    private fun updatePasswordView(password: Password, guessPasswordCombinations: Boolean, isObfuscated: Boolean) {
        getSecureActivity()?.let {
            generatedPassword = password
            masterSecretKey?.let { key ->
                currentCredential.password = SecretService.encryptPassword(key, generatedPassword)
            }

            if (!generatedPassword.isEmpty()) {
                var spannedString =
                    PasswordColorizer.spannableObfusableString(
                        generatedPassword,
                        passwordPresentation,
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
        credential: EncCredential,
        saveLastPassword: Boolean
    ) {
        val encPassword = SecretService.encryptPassword(key, generatedPassword)
        generatedPassword.clear()

        if (saveLastPassword) {
            editCredentialActivity.original?.let { original ->
                credential.backupForRestore(original)
            }
        }
        credential.password = encPassword
        LabelService.defaultHolder.updateLabelsForCredential(key, credential)

        editCredentialActivity.credentialViewModel.updateExpiredCredential(credential, key, editCredentialActivity, considerExpiredForThePast = true)

        editCredentialActivity.reply(obfuscationKey)
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
        return PassphraseGeneratorSpec(
            strength = passphraseStrength,
            wordBeginningUpperCase = switchUpperCaseChar.isChecked,
            addDigit = switchAddDigit.isChecked,
            addSpecialChar = switchAddSpecialChar.isChecked,
            useExtendedSpecialChars = PreferenceService.getAsBool(PREF_USE_EXTENDED_SPECIAL_CHARS, null),
        )
    }

    private fun buildPasswordGeneratorSpec(): PasswordGeneratorSpec {
        val passwordStrength = when (radioStrength.checkedRadioButtonId) {
            R.id.radio_strength_normal -> SecretStrength.NORMAL
            R.id.radio_strength_strong -> SecretStrength.STRONG
            R.id.radio_strength_super_strong -> SecretStrength.ULTRA
            R.id.radio_strength_extreme -> SecretStrength.EXTREME
            else -> PASSWORD_STRENGTH_DEFAULT // default
        }
        return PasswordGeneratorSpec(
            strength = passwordStrength,
            noUpperCase = !switchUpperCaseChar.isChecked,
            noDigits = !switchAddDigit.isChecked,
            noSpecialChars = !switchAddSpecialChar.isChecked,
            useExtendedSpecialChars = PreferenceService.getAsBool(PREF_USE_EXTENDED_SPECIAL_CHARS, null),
        )
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
                    toastText(activity, R.string.generate_password_first)
                }
                else {
                    val encPassword = SecretService.encryptPassword(key, generatedPassword)
                    DetachHelper.detachPassword(editCredentialActivity,
                        currentCredential.user,
                        encPassword,
                        null,
                        passwordPresentation)


                }
            }

            return true
        }

        if (id == R.id.menu_copy_credential) {
            masterSecretKey?.let{ key ->
                if (generatedPassword.isEmpty()) {
                    toastText(activity, R.string.generate_password_first)
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

                val lastPasswd = currentCredential.lastPassword?.let {
                    SecretService.decryptPassword(key, it)
                }
                if (lastPasswd == null || lastPasswd.isBlank() || !lastPasswd.isValid()) {
                    toastText(activity, R.string.nothing_to_restore)
                }
                else {
                    currentCredential.restore()
                    unsetObfuscation(currentCredential)
                    updatePasswordView(lastPasswd, guessPasswordCombinations = true,
                        currentCredential.isObfuscated)
                }
            }

            return true
        }

        if (id == R.id.menu_deobfuscate_password) {

            masterSecretKey?.let{ key ->

                if (obfuscationKey != null) {
                    obfuscationKey?.let {
                        generatedPassword.obfuscate(it)
                        currentCredential.isObfuscated = true
                        updatePasswordView(generatedPassword, guessPasswordCombinations = true,
                            currentCredential.isObfuscated)
                    }

                    item.isChecked = false
                    unsetObfuscation(currentCredential)

                    toastText(context, R.string.deobfuscate_restored)
                }
                else {
                    context?.let { ctx ->
                        DeobfuscationDialog.openDeobfuscationDialogForCredentials(ctx) { newObfuscationKey ->
                            if (newObfuscationKey == null) {
                                return@openDeobfuscationDialogForCredentials
                            }

                            item.isChecked = true

                            obfuscationKey = newObfuscationKey
                            obfuscationKey?.let {
                                generatedPassword.deobfuscate(it)
                                currentCredential.isObfuscated = false
                                updatePasswordView(generatedPassword, guessPasswordCombinations = true,
                                    currentCredential.isObfuscated)
                            }
                            updateObfuscationMenuItems(currentCredential)

                            toastText(ctx, R.string.password_deobfuscated)
                        }
                    }
                }
            }
            return true
        }

        if (id == R.id.menu_obfuscate_password) {
            obfuscatePasswordRequired = !obfuscatePasswordRequired
            updateObfuscationRequired(item)

            if (obfuscatePasswordRequired) {
                toastText(context, R.string.obfuscate_while_saving_toast)
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun updateObfuscationRequired(item: MenuItem) {
        item.isChecked = obfuscatePasswordRequired
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_credential_edit, menu)

        optionsMenu = menu

        val enableCopyPassword = PreferenceService.getAsBool(PreferenceService.PREF_ENABLE_COPY_PASSWORD, getBaseActivity())
        if (!enableCopyPassword) {
            menu.findItem(R.id.menu_copy_credential)?.isVisible = false
        }

        val enableOverlayFeature = PreferenceService.getAsBool(PreferenceService.PREF_ENABLE_OVERLAY_FEATURE, getBaseActivity())
        val inAutofillMode = editCredentialActivity.shouldPushBackAutoFill()
        if (inAutofillMode || !enableOverlayFeature) {
            menu.findItem(R.id.menu_detach_credential)?.isVisible = false
        }

        updateObfuscationMenuItems(editCredentialActivity.current)
    }

    private fun unsetObfuscation(credential: EncCredential?) {
        obfuscationKey?.clear()
        obfuscationKey = null
        updateObfuscationMenuItems(credential)
    }

    private fun updateObfuscationMenuItems(credential: EncCredential?) {
        val itemObfuscatePassword = optionsMenu?.findItem(R.id.menu_obfuscate_password)
        val itemDeobfuscatePassword = optionsMenu?.findItem(R.id.menu_deobfuscate_password)
        if (credential == null) {
            itemObfuscatePassword?.isVisible = true
            itemDeobfuscatePassword?.isVisible = false
        }
        credential?.let {
            if (it.isObfuscated) {
                itemObfuscatePassword?.isVisible = false
                itemDeobfuscatePassword?.isVisible = true
            }
            else {
                itemObfuscatePassword?.isVisible = true
                itemDeobfuscatePassword?.isVisible = false
            }
        }

        itemObfuscatePassword?.let { updateObfuscationRequired(it) }
    }


}