package de.jepfa.yapm.ui.credential

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuCompat
import androidx.core.view.children
import androidx.core.view.setPadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.OtpData
import de.jepfa.yapm.model.otp.OTPConfig
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_ENABLE_COPY_PASSWORD
import de.jepfa.yapm.service.PreferenceService.PREF_ENABLE_OVERLAY_FEATURE
import de.jepfa.yapm.service.PreferenceService.PREF_MASK_PASSWORD
import de.jepfa.yapm.service.PreferenceService.PREF_PASSWD_WORDS_ON_NL
import de.jepfa.yapm.service.autofill.AutofillCredentialHolder
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.decryptCommonString
import de.jepfa.yapm.service.secret.SecretService.decryptLong
import de.jepfa.yapm.service.secret.SecretService.decryptPassword
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.ui.label.LabelDialogs
import de.jepfa.yapm.usecase.credential.ExportCredentialUseCase
import de.jepfa.yapm.usecase.credential.ImportCredentialUseCase
import de.jepfa.yapm.usecase.credential.ShowPasswordStrengthUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.*
import de.jepfa.yapm.util.PasswordColorizer.spannableObfusableAndMaskableString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class ShowCredentialActivity : SecureActivity() {

    private enum class Mode {NORMAL, NORMAL_READONLY, EXTERNAL_FROM_RECORD, EXTERNAL_FROM_FILE}

    val updateCredentialActivityRequestCode = 1

    private var mode = Mode.NORMAL
    private var passwordPresentation = Password.FormattingStyle.DEFAULT
    private var maskPassword = false
    private var multiLine = false
    private var isAppBarAdjusted = false
    private var obfuscationKey: Key? = null
    private var credential: EncCredential? = null

    private lateinit var toolBarLayout: CollapsingToolbarLayout
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var titleLayout: LinearLayout
    private lateinit var toolbarChipGroup: ChipGroup
    private lateinit var passwordTextView: TextView
    private lateinit var userTextView: TextView
    private lateinit var websiteTextView: TextView
    private lateinit var expiresAtTextView: TextView
    private lateinit var additionalInfoTextView: TextView
    private lateinit var expandAdditionalInfoImageView: ImageView
    private var optionsMenu: Menu? = null
    private var defaultTextColor: ColorStateList? = null

    private lateinit var otpViewer: OtpViewer


    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_credential)

        val toolbar: Toolbar = findViewById(R.id.activity_credential_detail_toolbar)
        setSupportActionBar(toolbar)

        titleLayout = findViewById(R.id.collapsing_toolbar_layout_title)
        toolbarChipGroup = findViewById(R.id.toolbar_chip_group)
        titleLayout.post {
            // this is important to ensure labels are faded out when collapsing the app bar
            val layoutParams = toolbar.layoutParams as CollapsingToolbarLayout.LayoutParams
            layoutParams.height = titleLayout.height
            toolbar.layoutParams = layoutParams
        }

        val idExtra = intent.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)
        mode = when (intent.getStringExtra(EXTRA_MODE)) {
            EXTRA_MODE_SHOW_EXTERNAL_FROM_RECORD -> Mode.EXTERNAL_FROM_RECORD
            EXTRA_MODE_SHOW_EXTERNAL_FROM_FILE -> Mode.EXTERNAL_FROM_FILE
            EXTRA_MODE_SHOW_NORMAL_READONLY -> Mode.NORMAL_READONLY
            else -> Mode.NORMAL
        }

        maskPassword = PreferenceService.getAsBool(PREF_MASK_PASSWORD, this)
        val formatted =
            PreferenceService.getAsBool(PreferenceService.PREF_PASSWD_SHOW_FORMATTED, this)
        multiLine = PreferenceService.getAsBool(PREF_PASSWD_WORDS_ON_NL, this)
        passwordPresentation = Password.FormattingStyle.createFromFlags(multiLine, formatted)

        toolBarLayout = findViewById(R.id.credential_detail_toolbar_layout)
        appBarLayout = findViewById(R.id.credential_detail_appbar_layout)

        userTextView = findViewById(R.id.user)
        websiteTextView = findViewById(R.id.website)
        expiresAtTextView = findViewById(R.id.expires_at)

        additionalInfoTextView = findViewById(R.id.additional_info)
        val additionalInfoScrollView = findViewById<ScrollView>(R.id.additional_info_scroll_view)
        expandAdditionalInfoImageView = findViewById(R.id.imageview_expand_additional_info)

        expandAdditionalInfoImageView.setOnClickListener {
            expandAdditionalInfoView(expandAdditionalInfoImageView)
        }
        additionalInfoTextView.setOnClickListener {
            expandAdditionalInfoView(expandAdditionalInfoImageView)
        }
        additionalInfoTextView.setOnScrollChangeListener{ _, _, _, _, _ ->
            expandAdditionalInfoView(expandAdditionalInfoImageView)
        }
        additionalInfoScrollView.setOnScrollChangeListener{ _, _, _, _, _ ->
            expandAdditionalInfoView(expandAdditionalInfoImageView)
        }

        passwordTextView = findViewById(R.id.passwd)

        passwordTextView.setOnLongClickListener {
            showPasswordStrength()
            return@setOnLongClickListener true
        }
        passwordTextView.setOnClickListener {
            if (maskPassword) {
                maskPassword = false
            } else {
                passwordPresentation =
                    if (multiLine) passwordPresentation.prev()
                    else passwordPresentation.next()
            }
            masterSecretKey?.let { key ->
                credential?.let { credential ->
                    updatePasswordTextView(key, credential, true)
                }
            }
        }


        otpViewer = OtpViewer(null, this, hotpCounterChanged = {
                // store changed HOTP counter
                val otpAuthUri = otpViewer.otpConfig?.toUri()
                if (otpAuthUri != null) {
                    masterSecretKey?.let { key ->
                        credential?.let { current ->
                            val encUri = SecretService.encryptCommonString(key, otpAuthUri.toString())
                            current.otpData = OtpData(encUri)
                            credentialViewModel.update(current, this)
                        }

                    }

                }
            },
            masked = PreferenceService.getAsBool(PREF_MASK_PASSWORD, this))


        if (mode != Mode.NORMAL) {

            credential = EncCredential.fromIntent(intent)
            credential?.let {
                updatePasswordView(it)
            }
        } else {
            credentialViewModel.getById(idExtra).observe(this, {
                credential = it

                updatePasswordView(it)
            })
        }



        val otpToRestore = savedInstanceState?.getString("OTP")
        if (otpToRestore != null) {
            val otpConfig = OTPConfig.fromUri(Uri.parse(otpToRestore))
            otpConfig?.let {
                otpViewer.otpConfig = it
                otpViewer.start()

                otpViewer.refreshVisibility()
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        otpViewer.otpConfig?.let {
            outState.putString("OTP", it.toUri().toString())
        }
    }


    private fun showPasswordStrength() {
        if (credential != null) {
            masterSecretKey?.let { key ->
                val password = decryptPassword(key, credential!!.passwordData.password)
                obfuscationKey?.let {
                    password.deobfuscate(it)
                }
                val input = ShowPasswordStrengthUseCase.Input(password, R.string.password_strength)
                CoroutineScope(Dispatchers.Main).launch {
                    ShowPasswordStrengthUseCase.execute(input, this@ShowCredentialActivity)
                    password.clear()
                }
            }
        }
    }

    private fun expandAdditionalInfoView(expandAdditionalInfoImageView: ImageView) {
        appBarLayout.setExpanded(false, true)
        expandAdditionalInfoImageView.visibility = View.GONE
        additionalInfoTextView.maxLines = R.integer.max_credential_additional_info_length
    }

    override fun onResume() {
        super.onResume()
        credential?.let { AutofillCredentialHolder.update(it, obfuscationKey) }

        val requestReload = PreferenceService.getAsBool(PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_RELOAD, applicationContext)
        if (requestReload) {
            recreate()
            PreferenceService.delete(PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_RELOAD, applicationContext)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuId = if (mode == Mode.EXTERNAL_FROM_FILE || mode == Mode.NORMAL_READONLY) {
            R.menu.menu_credential_detail_raw
        } else if (mode == Mode.EXTERNAL_FROM_RECORD) {
            R.menu.menu_credential_detail_import
        } else {
            R.menu.menu_credential_detail
        }
        inflateActionsMenu(menu, menuId)
        MenuCompat.setGroupDividerEnabled(menu, true)


        val enableCopyPassword = PreferenceService.getAsBool(PREF_ENABLE_COPY_PASSWORD, this)
        if (!enableCopyPassword) {
            menu.findItem(R.id.menu_copy_credential)?.isVisible = false
        }

        val enableOverlayFeature = PreferenceService.getAsBool(PREF_ENABLE_OVERLAY_FEATURE, this)
        if (!enableOverlayFeature) {
            menu.findItem(R.id.menu_detach_credential)?.isVisible = false
        }

        menu.findItem(R.id.menu_deobfuscate_password)?.isVisible = credential?.passwordData?.isObfuscated ?: false


        optionsMenu = menu

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return false
        }

        credential?.let { credential ->
            if (id == R.id.menu_export_credential) {
                ExportCredentialUseCase.openStartExportDialog(credential, obfuscationKey, this)
                return true
            }

            if (id == R.id.menu_import_credential) {
                val input = ImportCredentialUseCase.Input(credential)
                {
                    val upIntent = Intent(this, ListCredentialsActivity::class.java)
                    navigateUpTo(upIntent)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    ImportCredentialUseCase.execute(input, this@ShowCredentialActivity)
                }
                return true
            }

            if (id == R.id.menu_lock_items) {
                LockVaultUseCase.execute(this)
                return true
            }

            if (id == R.id.menu_detach_credential) {
                DetachHelper.detachPassword(
                    this,
                    credential.user,
                    credential.passwordData.password,
                    obfuscationKey,
                    passwordPresentation
                )
                return true
            }

            if (id == R.id.menu_copy_credential) {
                ClipboardUtil.copyEncPasswordWithCheck(credential.passwordData.password, obfuscationKey, this)
                return true
            }

            if (id == R.id.menu_change_credential) {

                val intent = Intent(this, EditCredentialActivity::class.java)
                intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, credential.id)

                startActivityForResult(intent, updateCredentialActivityRequestCode)

                return true
            }

            if (id == R.id.menu_delete_credential) {

                masterSecretKey?.let { key ->
                    val decName = decryptCommonString(key, credential.name)
                    val name = enrichId(this, decName, credential.id)

                    AlertDialog.Builder(this)
                        .setTitle(R.string.title_delete_credential)
                        .setMessage(getString(R.string.message_delete_credential, name))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                            credential.id?.let { id ->
                                credentialViewModel.deleteCredentialExpiry(id, this)
                            }
                            credentialViewModel.delete(credential)
                            toastText(this, R.string.credential_deleted)

                            val upIntent = Intent(this, ListCredentialsActivity::class.java)
                            navigateUpTo(upIntent)
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
                }
                return true
            }

            if (id == R.id.menu_deobfuscate_password) {

                masterSecretKey?.let { key ->

                    if (obfuscationKey != null) {
                        obfuscationKey?.clear()
                        obfuscationKey = null
                        item.isChecked = false

                        AutofillCredentialHolder.clear()

                        updatePasswordTextView(key, credential, false)

                        toastText(this, R.string.deobfuscate_restored)
                    } else {

                        DeobfuscationDialog.openDeobfuscationDialogForCredentials(this) { deobfuscationKey ->
                            if (deobfuscationKey == null) {
                                return@openDeobfuscationDialogForCredentials
                            }

                            maskPassword = false
                            item.isChecked = true

                            obfuscationKey = deobfuscationKey
                            obfuscationKey?.let {
                                val passwordForDeobfuscation = decryptPassword(key, credential.passwordData.password)
                                passwordForDeobfuscation.deobfuscate(it)

                                var spannedString =
                                    spannableObfusableAndMaskableString(
                                        passwordForDeobfuscation,
                                        passwordPresentation,
                                        maskPassword,
                                        showObfuscated(credential),
                                        this
                                    )
                                passwordForDeobfuscation.clear()

                                passwordTextView.text = spannedString
                                AutofillCredentialHolder.update(credential, it)
                            }

                            toastText(this, R.string.password_deobfuscated)
                        }
                    }
                }
                return true
            }

            if (id == R.id.menu_password_strength) {
                showPasswordStrength()
                return true
            }

            if (id == R.id.menu_details) {
                val sb = StringBuilder()

                credential.id?.let { sb.addFormattedLine(getString(R.string.identifier), it)}
                credential.uid?.let {
                    sb.addFormattedLine(
                        getString(R.string.universal_identifier),
                        it.toReadableString())
                }

                masterSecretKey?.let { key ->
                    val name = decryptCommonString(key, credential.name)
                    sb.addFormattedLine(getString(R.string.name), name)
                }

                when (mode) {
                    Mode.EXTERNAL_FROM_FILE -> sb.addFormattedLine(getString(R.string.source), getString(R.string.source_from_file))
                    Mode.EXTERNAL_FROM_RECORD -> sb.addFormattedLine(getString(R.string.source), getString(R.string.source_from_record))
                    Mode.NORMAL_READONLY -> sb.addFormattedLine(getString(R.string.source), getString(R.string.source_from_the_app))
                    else -> {}
                }


                sb.addFormattedLine(getString(R.string.password_obfuscated),
                    if (credential.passwordData.isObfuscated) getString(R.string.yes)
                    else getString(R.string.no))


                credential.timeData.modifyTimestamp?.let{
                    if (it > 1000) // modifyTimestamp is the credential Id after running db migration, assume ids are lower than 1000
                        sb.addFormattedLine(getString(R.string.last_modified), dateTimeToNiceString(it.toDate(), this))
                }

                masterSecretKey?.let { key ->
                    val expiresAt = decryptLong(key, credential.timeData.expiresAt)
                    if (expiresAt != null && expiresAt != 0L) {
                        sb.addFormattedLine(
                            getString(R.string.expires),
                            dateTimeToNiceString(Date(expiresAt), this)
                        )
                    }
                }

                val builder = AlertDialog.Builder(this)
                    .setTitle(R.string.title_credential_details)
                    .setMessage(sb.toString())
                    .setNegativeButton(R.string.close, null)


                if (credential.uid != null) {
                    builder.setNeutralButton(R.string.copy_universal_identifier) { _, _ ->
                        credential.uid?.let { uid ->
                            ClipboardUtil.copy(
                                this.getString(R.string.universal_identifier),
                                uid.toReadableString(),
                                this,
                                isSensible = false,
                            )
                        }
                        toastText(this, R.string.universal_identifier_copied)
                    }
                }

                builder.show()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == updateCredentialActivityRequestCode && resultCode == Activity.RESULT_OK) {
            data?.let {

                val credential = EncCredential.fromIntent(it)
                if (credential.isPersistent()) {
                    obfuscationKey?.clear()
                    obfuscationKey = null
                    optionsMenu?.findItem(R.id.menu_deobfuscate_password)?.isChecked = false
                    credentialViewModel.update(credential, this)
                }
            }
        }

    }

    override fun lock() {
        obfuscationKey?.clear()
        obfuscationKey = null
        maskPassword = true
        recreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        obfuscationKey?.clear()
        otpViewer.stop()
    }

    private fun showObfuscated(credential: EncCredential): Boolean {
        return credential.passwordData.isObfuscated && obfuscationKey == null
    }


    @SuppressLint("SetTextI18n")
    private fun updatePasswordView(credential: EncCredential) {
        masterSecretKey?.let { key ->
            val decName = decryptCommonString(key, credential.name)
            val name = enrichId(this, decName, credential.id)
            val user = decryptCommonString(key, credential.user)
            val website = decryptCommonString(key, credential.website)
            val expiresAt = decryptLong(key, credential.timeData.expiresAt)
            val additionalInfo = decryptCommonString(key, credential.additionalInfo)

            val otpData = credential.otpData

            if (otpData != null) {
                val otpAuthUri = decryptCommonString(key, otpData.encOtpAuthUri)

                otpViewer.otpConfig = OTPConfig.fromUri(Uri.parse(otpAuthUri))
                otpViewer.start()
                otpViewer.refreshVisibility()
            }
            else {
                otpViewer.otpConfig = null
                otpViewer.stop()
                otpViewer.refreshVisibility()
            }

            toolBarLayout.title = name
            toolbarChipGroup.removeAllViews()



            val labelHolder = if (mode == Mode.EXTERNAL_FROM_FILE) LabelService.externalHolder else LabelService.defaultHolder
            val labelsForCredential = labelHolder.decryptLabelsForCredential(key, credential)

            val credentialExpired = credential.isExpired(key)
            val thinner = shouldMakeLabelThinner(labelsForCredential, credentialExpired)

            if (credentialExpired) { // expired
                createAndAddLabelChip(
                    Label(getString(R.string.expired), getColor(R.color.Red), R.drawable.baseline_lock_clock_24),
                    toolbarChipGroup,
                    thinner,
                    this,
                    outlined = true,
                    placedOnAppBar = true,
                )
            }

            if (labelsForCredential.isNotEmpty()) {
                labelsForCredential.forEachIndexed { idx, label ->
                    val chip = createAndAddLabelChip(label, toolbarChipGroup, thinner, this)
                    chip.setOnClickListener {
                        val showChangeLabelButton = mode == Mode.NORMAL
                        LabelDialogs.openLabelOverviewDialog(this, label, showChangeLabelButton)
                    }
                }
            } else {
                // add blind label to ensure layouting
                val blindView = TextView(this)
                blindView.setPadding(16)
                titleLayout.addView(blindView)
                if (!isAppBarAdjusted) {
                    appBarLayout.layoutParams.height -= 50
                    isAppBarAdjusted = true
                }
            }

            val userImageView: ImageView = findViewById(R.id.user_image)
            if (user.isEmpty()) {
                userImageView.visibility = View.GONE
                userTextView.visibility = View.GONE
            }
            else {
                userImageView.visibility = View.VISIBLE
                userTextView.visibility = View.VISIBLE
            }
            userTextView.text = user

            val websiteImageView: ImageView = findViewById(R.id.website_image)
            websiteTextView.text = website
            if (website.isEmpty()) {
                websiteImageView.visibility = View.GONE
                websiteTextView.visibility = View.GONE
            } else {
                websiteImageView.visibility = View.VISIBLE
                websiteTextView.visibility = View.VISIBLE
                linkify(websiteTextView)
            }

            val expiresAtImageView: ImageView = findViewById(R.id.expires_at_image)
            if (expiresAt != null && expiresAt > 0) {
                val expiryDate = Date(expiresAt)
                if (!credentialExpired) {
                    // good
                    expiresAtTextView.text = "${getString(R.string.expires)}: ${dateToNiceString(expiryDate, this)}"
                    expiresAtTextView.typeface = Typeface.DEFAULT
                    defaultTextColor?.let {
                        expiresAtTextView.setTextColor(it)
                    }
                }
                else {
                    // expired
                    expiresAtTextView.text = "${getString(R.string.expired_since)}: ${dateToNiceString(expiryDate, this, withPreposition = false)}!"
                    expiresAtTextView.typeface = Typeface.DEFAULT_BOLD
                    defaultTextColor = expiresAtTextView.textColors
                    expiresAtTextView.setTextColor(getColor(R.color.Red))
                }
                expiresAtImageView.visibility = View.VISIBLE
                expiresAtTextView.visibility = View.VISIBLE
            } else {
                expiresAtImageView.visibility = View.GONE
                expiresAtTextView.visibility = View.GONE
            }

            additionalInfoTextView.text = additionalInfo
            expandAdditionalInfoImageView.visibility =
                if (additionalInfo.lines().count() > 3) View.VISIBLE else View.INVISIBLE

            updatePasswordTextView(key, credential, true)

            optionsMenu?.findItem(R.id.menu_deobfuscate_password)?.isVisible =
                credential.passwordData.isObfuscated

            if (DebugInfo.isDebug) {
                titleLayout.setOnLongClickListener {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    val icon: Drawable = resources.getDrawable(R.mipmap.ic_logo)
                    val labelIds = toolbarChipGroup.children
                        .mapNotNull { it as? Chip }
                        .mapNotNull { it.tag as? Int }
                        .sorted()
                        .toList()
                    val message = credential.toString() + System.lineSeparator() + "LabelIds: " + labelIds
                    builder.setTitle(R.string.debug)
                        .setMessage(message)
                        .setIcon(icon)
                        .show()
                    true
                }
            }
        }

        AutofillCredentialHolder.update(credential, obfuscationKey)
    }

    private fun updatePasswordTextView(
        key: SecretKeyHolder,
        credential: EncCredential,
        allowDeobfuscate: Boolean
    ) {
        val password = decryptPassword(key, credential.passwordData.password)
        if (allowDeobfuscate) {
            obfuscationKey?.let {
                password.deobfuscate(it) //TODO this seems to cause a change of current item and a credential adapter list reload
            }
        }
        val spannedString = spannableObfusableAndMaskableString(
            password,
            passwordPresentation,
            maskPassword,
            showObfuscated(credential),
            this
        )
        passwordTextView.text = spannedString
        password.clear()
    }

    private fun shouldMakeLabelThinner(labels: List<Label>, credentialExpired: Boolean): Boolean {
        var totalLabelsLength = labels.sumOf { it.name.length }
        if (credentialExpired) {
            totalLabelsLength+= getString(R.string.expired).length
        }
        val maxLabelLength = resources.getInteger(R.integer.max_label_name_length)
        return totalLabelsLength > maxLabelLength * 2
    }

    companion object {
        const val EXTRA_MODE = "de.jepfa.yapm.ui.ShowCredentialActivity.mode"
        const val EXTRA_MODE_SHOW_NORMAL =
            "de.jepfa.yapm.ui.ShowCredentialActivity.mode.show_normal"
        const val EXTRA_MODE_SHOW_NORMAL_READONLY =
            "de.jepfa.yapm.ui.ShowCredentialActivity.mode.show_normal_readonly"
        const val EXTRA_MODE_SHOW_EXTERNAL_FROM_RECORD =
            "de.jepfa.yapm.ui.ShowCredentialActivity.mode.show_external_from_record"
        const val EXTRA_MODE_SHOW_EXTERNAL_FROM_FILE =
            "de.jepfa.yapm.ui.ShowCredentialActivity.mde.show_external_from_vault_file"
    }

}