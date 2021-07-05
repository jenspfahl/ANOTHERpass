package de.jepfa.yapm.ui.login

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_ENCRYPTED_MASTER_KEY
import de.jepfa.yapm.service.PreferenceService.PREF_MAX_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.PreferenceService.PREF_SELF_DESTRUCTION
import de.jepfa.yapm.service.PreferenceService.STATE_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.ui.importvault.ImportVaultActivity
import de.jepfa.yapm.ui.nfc.NfcBaseActivity
import de.jepfa.yapm.usecase.DropVaultUseCase
import de.jepfa.yapm.usecase.ShowInfoUseCase
import de.jepfa.yapm.util.Constants


class LoginActivity : NfcBaseActivity() {

    var loginAttempts = 0
    var showTagDetectedMessage = false

    val createVaultActivityRequestCode = 1
    val importVaultActivityRequestCode = 2


    init {
        checkSession = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)

        loginAttempts = PreferenceService.getAsInt(STATE_LOGIN_ATTEMPTS,  this)

        if (MasterKeyService.isMasterKeyStored(this)) {
            setContentView(R.layout.activity_login)
            nfcAdapter = NfcService.getNfcAdapter(this)
            readTagFromIntent(intent)

        }
        else {
            setContentView(R.layout.activity_create_or_import_vault)
            val buttonCreateVault: Button = findViewById(R.id.button_create_vault)
            buttonCreateVault.setOnClickListener {

                if (MasterKeyService.isMasterKeyStored(this)) {
                    Toast.makeText(this, getString(R.string.vault_already_created), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val intent = Intent(this@LoginActivity, CreateVaultActivity::class.java)
                startActivityForResult(intent, importVaultActivityRequestCode)
            }
            val buttonImportVault: Button = findViewById(R.id.button_import_vault)
            buttonImportVault.setOnClickListener {
                if (MasterKeyService.isMasterKeyStored(this)) {
                    Toast.makeText(this, getString(R.string.vault_already_created), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val intent = Intent(this@LoginActivity, ImportVaultActivity::class.java)
                startActivityForResult(intent, createVaultActivityRequestCode)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_login_help) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Constants.HOMEPAGE)
            startActivity(browserIntent)
            return true
        }

        if (id == R.id.action_reset_vault) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_reset_all))
                .setMessage(getString(R.string.message_extract_all))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.reset_vault) { dialog, whichButton ->
                    DropVaultUseCase.dropVaultData(this)
                    Session.logout()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
        }

        if (id == R.id.menu_about) {
            ShowInfoUseCase.execute(this)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == createVaultActivityRequestCode && resultCode == Activity.RESULT_OK) {
            recreate()
        }
        if (requestCode == importVaultActivityRequestCode && resultCode == Activity.RESULT_OK) {
            recreate()
        }
    }

    fun handleFailedLoginAttempt() {
        loginAttempts++
        PreferenceService.putString(STATE_LOGIN_ATTEMPTS, loginAttempts.toString(), this)
        if (loginAttempts >= getMaxLoginAttempts()) {
            val selfDestruction = PreferenceService.getAsBool(PREF_SELF_DESTRUCTION, this)

            if (selfDestruction) {
                Toast.makeText(baseContext, R.string.too_may_wrong_logins_self_destruction, Toast.LENGTH_LONG).show()
                DropVaultUseCase.dropVaultData(this)
            }
            else {
                Toast.makeText(baseContext, R.string.too_may_wrong_logins, Toast.LENGTH_LONG).show()
                PreferenceService.delete(PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD, baseContext)
                PreferenceService.delete(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, baseContext)
            }
            Session.logout()
            finishAffinity()
            finishAndRemoveTask()
        }
    }

    override fun handleTag() {
        Log.i("LOGIN", "tag detected " + ndefTag?.tagId)
        if (ndefTag != null && showTagDetectedMessage) {
            Toast.makeText(this, getString(R.string.nfc_tag_for_login_detected), Toast.LENGTH_LONG).show()
        }
    }

    override fun lock() {

    }

    fun getLoginAttemptMessage(): String {
        return getString(R.string.login_attempt, loginAttempts, getMaxLoginAttempts())
    }

    fun loginSuccessful() {
        loginAttempts = 0
        PreferenceService.delete(STATE_LOGIN_ATTEMPTS, this)

        val isFromAutofill = intent.getBooleanExtra(SecretChecker.fromAutofill, false)
        if (isFromAutofill) {
            setResult(SecretChecker.loginRequestCode, intent)
        }
        else {
            val intent = Intent(this, ListCredentialsActivity::class.java)
            startActivity(intent)
        }
        finish()
    }

    private fun getMaxLoginAttempts(): Int {
        return PreferenceService.getAsInt(PREF_MAX_LOGIN_ATTEMPTS, this)
    }
}