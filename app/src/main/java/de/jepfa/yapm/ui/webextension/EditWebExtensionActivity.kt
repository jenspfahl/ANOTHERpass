package de.jepfa.yapm.ui.webextension

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.getIntExtra


class EditWebExtensionActivity : SecureActivity() {

    private var webExtension: EncWebExtension? = null
    private lateinit var webClientIdTextView: TextView
    private lateinit var titleTextView: TextView
    private lateinit var enabledSwitchView: SwitchCompat

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_web_extension)

        webClientIdTextView = findViewById(R.id.web_extension_client_id)
        titleTextView = findViewById(R.id.edit_web_extension_title)
        enabledSwitchView = findViewById(R.id.switch_web_extension_enabled)


        enabledSwitchView.setOnCheckedChangeListener { _, isChecked ->
            hideKeyboard(titleTextView)


        }



        val webExtensionId = intent.getIntExtra(EncWebExtension.EXTRA_WEB_EXTENSION_ID)
        if (webExtensionId != null) {
            webExtensionViewModel.getById(webExtensionId).observe(this) { encWebExtension ->
                webExtension = encWebExtension
                masterSecretKey?.let { key ->
                    webClientIdTextView.text = SecretService.decryptCommonString(key, encWebExtension.webClientId)
                    titleTextView.text = encWebExtension.title?.let { SecretService.decryptCommonString(key, it) } ?: ""
                    enabledSwitchView.isChecked = encWebExtension.enabled

                }
            }
            setTitle(R.string.title_change_username_template) //TODO
        }
        else {
            setTitle(R.string.title_new_username_template) // TODO
        }



        val saveButton: Button = findViewById(R.id.button_save)
        saveButton.setOnClickListener {

            masterSecretKey?.let { key ->

                webExtension?.title = SecretService.encryptCommonString(key, titleTextView.text.toString())
                webExtension?.enabled = enabledSwitchView.isChecked

                webExtension?.let { webExtension ->
                    if (webExtension.isPersistent()) {
                        webExtensionViewModel.update(webExtension, this)
                    } else {
                        webExtensionViewModel.insert(webExtension, this)
                    }
                }
            }

            finish()

        }

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (checkSession && Session.isDenied()) {
            return false
        }

        if (webExtension != null) {
            menuInflater.inflate(R.menu.menu_username_template_edit, menu) //TODO
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
            webExtension?.let { current ->
               //TODO WebExtensionDialogs.openDeleteWebExtension(current, this, finishActivityAfterDelete = true)
            }

            return true
        }


        return super.onOptionsItemSelected(item)
    }


    override fun lock() {
        finish()
    }

}