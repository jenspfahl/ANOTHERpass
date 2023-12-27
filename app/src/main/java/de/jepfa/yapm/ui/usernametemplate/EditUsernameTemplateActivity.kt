package de.jepfa.yapm.ui.usernametemplate

import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
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

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_username_template)

        templateUsernameTextView = findViewById(R.id.edit_username_template_username)
        templateDescriptionTextView = findViewById(R.id.edit_username_template_description)


        val usernameTemplateId = intent.getIntExtra(EncUsernameTemplate.EXTRA_USERNAME_TEMPLATE_ID)
        if (usernameTemplateId != null) {
            usernameTemplateViewModel.getById(usernameTemplateId).observe(this) {
                usernameTemplate = it
                masterSecretKey?.let { key ->
                    templateUsernameTextView.text = SecretService.decryptCommonString(key, it.username)
                    templateDescriptionTextView.text = SecretService.decryptCommonString(key, it.description)

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

            masterSecretKey?.let { key ->
                val usernameTemplate = EncUsernameTemplate(usernameTemplateId,
                    SecretService.encryptCommonString(key, templateUsernameTextView.text.toString()),
                    SecretService.encryptCommonString(key, templateDescriptionTextView.text.toString()),
                    SecretService.encryptLong(key, EncUsernameTemplate.GeneratorType.NONE.ordinal.toLong()))

                if (usernameTemplate.isPersistent()) {
                    usernameTemplateViewModel.update(usernameTemplate, this)
                } else {
                    usernameTemplateViewModel.insert(usernameTemplate, this)
                }
            }

            finish()

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


}