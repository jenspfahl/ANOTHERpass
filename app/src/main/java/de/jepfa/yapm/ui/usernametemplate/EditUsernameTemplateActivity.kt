package de.jepfa.yapm.ui.usernametemplate

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncUsernameTemplate
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.getIntExtra


class EditUsernameTemplateActivity : SecureActivity() {

    private var usernameTemplate: EncUsernameTemplate? = null
    private lateinit var templateUsernameTextView: TextView
    private lateinit var templateDescriptionTextView: TextView
    private lateinit var templateTypeSwitchView: SwitchCompat
    private lateinit var templateGeneratorTypeGroup: RadioGroup
    private var generatorType: EncUsernameTemplate.GeneratorType = EncUsernameTemplate.GeneratorType.NONE

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_username_template)

        templateUsernameTextView = findViewById(R.id.edit_username_template_username)
        templateDescriptionTextView = findViewById(R.id.edit_username_template_description)
        templateTypeSwitchView = findViewById(R.id.switch_use_email_alias)
        templateGeneratorTypeGroup = findViewById(R.id.radio_generator_type)

        findViewById<ImageView>(R.id.imageview_use_email_alias_help).setOnClickListener{
            AlertDialog.Builder(this)
                .setTitle(R.string.title_username_email_alias)
                .setMessage(R.string.message_username_email_alias)
                .show()
        }

        templateTypeSwitchView.setOnCheckedChangeListener { _, isChecked ->
            hideKeyboard(templateUsernameTextView)
            hideKeyboard(templateDescriptionTextView)

            if (isChecked) {
                templateGeneratorTypeGroup.visibility = View.VISIBLE
                if (generatorType == EncUsernameTemplate.GeneratorType.NONE) {
                    generatorType = EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_CREDENTIAL_NAME_BASED
                }
                getRadioGroupIndex(generatorType)?.let {
                    templateGeneratorTypeGroup.check(it)
                }
            }
            else {
                templateGeneratorTypeGroup.visibility = View.INVISIBLE
                generatorType = EncUsernameTemplate.GeneratorType.NONE
            }
        }

        templateGeneratorTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            hideKeyboard(templateUsernameTextView)
            hideKeyboard(templateDescriptionTextView)
            when (checkedId) {
                R.id.radio_generator_type_from_credential -> generatorType = EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_CREDENTIAL_NAME_BASED
                R.id.radio_generator_type_with_random_word -> generatorType = EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_RANDOM_BASED
                R.id.radio_generator_type_both -> generatorType = EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_BOTH
            }
        }


        val usernameTemplateId = intent.getIntExtra(EncUsernameTemplate.EXTRA_USERNAME_TEMPLATE_ID)
        if (usernameTemplateId != null) {
            usernameTemplateViewModel.getById(usernameTemplateId).observe(this) { encUsernameTemplate ->
                usernameTemplate = encUsernameTemplate
                masterSecretKey?.let { key ->
                    templateUsernameTextView.text = SecretService.decryptCommonString(key, encUsernameTemplate.username)
                    templateDescriptionTextView.text = SecretService.decryptCommonString(key, encUsernameTemplate.description)
                    val generatorTypeIdx = SecretService.decryptLong(key, encUsernameTemplate.generatorType) ?: 0
                    generatorType = EncUsernameTemplate.GeneratorType.values()[generatorTypeIdx.toInt()]
                    templateTypeSwitchView.isChecked = generatorType != EncUsernameTemplate.GeneratorType.NONE
                    getRadioGroupIndex(generatorType)?.let {
                        templateGeneratorTypeGroup.check(it)
                    }

                }
            }
            setTitle(R.string.title_change_username_template)
        }
        else {
            setTitle(R.string.title_new_username_template)
        }



        val saveButton: Button = findViewById(R.id.button_save)
        saveButton.setOnClickListener {
            if (TextUtils.isEmpty(templateUsernameTextView.text)) {
                templateUsernameTextView.error = getString(R.string.error_field_required)
                templateUsernameTextView.requestFocus()
                return@setOnClickListener
            }
            if (generatorType != EncUsernameTemplate.GeneratorType.NONE && !isEmailAddress(templateUsernameTextView.text)) {
                templateUsernameTextView.error = getString(R.string.error_not_an_email_address)
                templateUsernameTextView.requestFocus()
                return@setOnClickListener
            }

            masterSecretKey?.let { key ->
                val usernameTemplate = EncUsernameTemplate(usernameTemplateId,
                    SecretService.encryptCommonString(key, templateUsernameTextView.text.toString()),
                    SecretService.encryptCommonString(key, templateDescriptionTextView.text.toString()),
                    SecretService.encryptLong(key, generatorType.ordinal.toLong()))

                if (usernameTemplate.isPersistent()) {
                    usernameTemplateViewModel.update(usernameTemplate, this)
                } else {
                    usernameTemplateViewModel.insert(usernameTemplate, this)
                }
            }

            finish()

        }

    }

    private fun isEmailAddress(text: CharSequence?): Boolean {
        return if (TextUtils.isEmpty(text)) {
            false;
        } else {
            android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches();
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (checkSession && Session.isDenied()) {
            return false
        }

        if (usernameTemplate != null) {
            menuInflater.inflate(R.menu.menu_username_template_edit, menu)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (checkSession && Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return false
        }

        if (id == R.id.menu_delete_username_template) {
            usernameTemplate?.let { current ->
                UsernameTemplateDialogs.openDeleteUsernameTemplate(current, this, finishActivityAfterDelete = true)
            }

            return true
        }


        return super.onOptionsItemSelected(item)
    }


    override fun lock() {
        finish()
    }

    private fun getRadioGroupIndex(generatorType: EncUsernameTemplate.GeneratorType): Int? {
        return when (generatorType) {
            EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_CREDENTIAL_NAME_BASED -> R.id.radio_generator_type_from_credential
            EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_RANDOM_BASED -> R.id.radio_generator_type_with_random_word
            EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_BOTH -> R.id.radio_generator_type_both
            EncUsernameTemplate.GeneratorType.NONE -> null
        }
    }

}