package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.setPadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.chip.ChipGroup
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
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
import de.jepfa.yapm.service.secret.SecretService.decryptCommonString
import de.jepfa.yapm.service.secret.SecretService.decryptPassword
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.ui.label.LabelDialogs
import de.jepfa.yapm.usecase.credential.ExportCredentialUseCase
import de.jepfa.yapm.usecase.credential.ImportCredentialUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.*
import de.jepfa.yapm.util.PasswordColorizer.spannableObfusableAndMaskableString


class ShowCredentialActivity : SecureActivity() {

    private enum class Mode {NORMAL, EXTERNAL_FROM_RECORD, EXTERNAL_FROM_FILE}

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
    private lateinit var additionalInfoTextView: TextView
    private var optionsMenu: Menu? = null

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
            EXTRA_MODE_SHOW_EXTERNAL_FROM_VAULT_FILE -> Mode.EXTERNAL_FROM_FILE
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
        additionalInfoTextView = findViewById(R.id.additional_info)
        passwordTextView = findViewById(R.id.passwd)

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

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
        if (mode == Mode.EXTERNAL_FROM_FILE) {
            menuInflater.inflate(R.menu.menu_credential_detail_raw, menu)
        } else if (mode == Mode.EXTERNAL_FROM_RECORD) {
            menuInflater.inflate(R.menu.menu_credential_detail_import, menu)
        } else {
            menuInflater.inflate(R.menu.menu_credential_detail, menu)
        }

        val enableCopyPassword = PreferenceService.getAsBool(PREF_ENABLE_COPY_PASSWORD, this)
        if (!enableCopyPassword) {
            menu.findItem(R.id.menu_copy_credential)?.isVisible = false
        }

        val enableOverlayFeature = PreferenceService.getAsBool(PREF_ENABLE_OVERLAY_FEATURE, this)
        if (!enableOverlayFeature) {
            menu.findItem(R.id.menu_detach_credential)?.isVisible = false
        }

        menu.findItem(R.id.menu_deobfuscate_password)?.isVisible = credential?.isObfuscated ?: false


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
                ImportCredentialUseCase.execute(input, this)
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
                    credential.password,
                    obfuscationKey,
                    passwordPresentation
                )
                return true
            }

            if (id == R.id.menu_copy_credential) {
                ClipboardUtil.copyEncPasswordWithCheck(credential.password, obfuscationKey, this)
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

                        DeobfuscationDialog.openDeobfuscationDialog(this) { deobfuscationKey ->
                            maskPassword = false
                            item.isChecked = true

                            obfuscationKey = deobfuscationKey
                            obfuscationKey?.let {
                                val passwordForDeobfuscation = decryptPassword(key, credential.password)
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
    }

    private fun showObfuscated(credential: EncCredential): Boolean {
        return credential.isObfuscated && obfuscationKey == null
    }


    private fun updatePasswordView(credential: EncCredential) {
        masterSecretKey?.let { key ->
            val decName = decryptCommonString(key, credential.name)
            val name = enrichId(this, decName, credential.id)
            val user = decryptCommonString(key, credential.user)
            val website = decryptCommonString(key, credential.website)
            val additionalInfo = decryptCommonString(key, credential.additionalInfo)

            toolBarLayout.title = name

            toolbarChipGroup.removeAllViews()

            val labelHolder = if (mode == Mode.EXTERNAL_FROM_FILE) LabelService.externalHolder else LabelService.defaultHolder
            val labelsForCredential = labelHolder.decryptLabelsForCredential(key, credential)
            if (labelsForCredential.isNotEmpty()) {
                val thinner = shouldMakeLabelThinner(labelsForCredential)
                labelsForCredential.forEachIndexed { idx, label ->
                    val chip = createAndAddLabelChip(label, toolbarChipGroup, thinner, this)
                    chip.setOnClickListener { _ ->
                        LabelDialogs.openLabelOverviewDialog(this, label)
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

            if (user.isEmpty()) {
                val userView: ImageView = findViewById(R.id.user_image)
                userView.visibility = View.INVISIBLE
            }
            userTextView.text = user

            val websiteView: ImageView = findViewById(R.id.website_image)
            websiteTextView.text = website
            if (website.isEmpty()) {
                websiteView.visibility = View.INVISIBLE
            } else {
                websiteView.visibility = View.VISIBLE
                linkify(websiteTextView)
            }

            additionalInfoTextView.text = additionalInfo

            updatePasswordTextView(key, credential, true)

            optionsMenu?.findItem(R.id.menu_deobfuscate_password)?.isVisible =
                credential.isObfuscated

            if (DebugInfo.isDebug) {
                passwordTextView.setOnLongClickListener {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    val icon: Drawable = applicationInfo.loadIcon(packageManager)
                    val message = credential.toString()
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
        val password = decryptPassword(key, credential.password)
        if (allowDeobfuscate) {
            obfuscationKey?.let {
                password.deobfuscate(it)
            }
        }
        var spannedString = spannableObfusableAndMaskableString(
            password,
            passwordPresentation,
            maskPassword,
            showObfuscated(credential),
            this
        )
        passwordTextView.text = spannedString
        password.clear()
    }

    private fun shouldMakeLabelThinner(labels: List<Label>): Boolean {
        val totalLabelsLength = labels.map { it.name.length }.sum()
        val maxLabelLength = resources.getInteger(R.integer.max_label_name_length)
        return totalLabelsLength > maxLabelLength * 2
    }

    companion object {
        const val EXTRA_MODE = "de.jepfa.yapm.ui.ShowCredentialActivity.mode"
        const val EXTRA_MODE_SHOW_EXISTING =
            "de.jepfa.yapm.ui.ShowCredentialActivity.mode.show_existing"
        const val EXTRA_MODE_SHOW_EXTERNAL_FROM_RECORD =
            "de.jepfa.yapm.ui.ShowCredentialActivity.mode.show_external_from_record"
        const val EXTRA_MODE_SHOW_EXTERNAL_FROM_VAULT_FILE =
            "de.jepfa.yapm.ui.ShowCredentialActivity.mde.show_external_from_vault_file"
    }

}