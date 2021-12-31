package de.jepfa.yapm.ui.login

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.navigation.fragment.NavHostFragment
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.STATE_INTRO_SHOWED
import de.jepfa.yapm.service.PreferenceService.PREF_MAX_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.PreferenceService.PREF_SELF_DESTRUCTION
import de.jepfa.yapm.service.PreferenceService.STATE_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.*
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.ui.importvault.ImportVaultActivity
import de.jepfa.yapm.ui.nfc.NfcBaseActivity
import de.jepfa.yapm.ui.intro.IntroActivity
import de.jepfa.yapm.usecase.vault.DropVaultUseCase
import de.jepfa.yapm.usecase.app.ShowInfoUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.observeOnce
import de.jepfa.yapm.util.toastText


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

        val introShowed = PreferenceService.getAsBool(STATE_INTRO_SHOWED, this)
        val isFromAutofill = intent.getBooleanExtra(SecretChecker.fromAutofill, false)
        if (!introShowed && !isFromAutofill && Session.isLoggedOut()) {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
        }

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
                    toastText(this, R.string.vault_already_created)
                    return@setOnClickListener
                }

                val intent = Intent(this@LoginActivity, CreateVaultActivity::class.java)
                startActivityForResult(intent, importVaultActivityRequestCode)
            }
            val buttonImportVault: Button = findViewById(R.id.button_import_vault)
            buttonImportVault.setOnClickListener {
                if (MasterKeyService.isMasterKeyStored(this)) {
                    toastText(this, R.string.vault_already_created)
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

        if (id == R.id.menu_intro) {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            return true
        }

        if (id == R.id.action_reset_vault) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_reset_all))
                .setMessage(getString(R.string.message_reset_all))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.reset_vault) { dialog, whichButton ->
                    DropVaultUseCase.doubleCheckDropVault(this)
                        {dropAndLogoutVault()}
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
                toastText(baseContext, R.string.too_may_wrong_logins_self_destruction)
                DropVaultUseCase.dropVaultData(this)
            }
            else {
                toastText(baseContext, R.string.too_may_wrong_logins)
                MasterPasswordService.deleteStoredMasterPassword(baseContext)
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
            toastText(this, R.string.nfc_tag_for_login_detected)
        }

        val navFragment = supportFragmentManager.primaryNavigationFragment
        if (navFragment != null && navFragment is NavHostFragment) {
            val currFragment = navFragment.childFragmentManager.primaryNavigationFragment
            if (currFragment is LoginEnterMasterPasswordFragment) {
                currFragment.updateMasterKeyFromNfcTag()
            }
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

    internal fun readMasterPassword(scanned: String): Password? {
        val encrypted = Encrypted.fromEncryptedBase64StringWithCheck(scanned)
        return when (encrypted?.type?.type) {
            EncryptedType.Types.ENC_MASTER_PASSWD -> readEMP(encrypted)
            EncryptedType.Types.MASTER_PASSWD_TOKEN -> readMPT(encrypted)
            else -> {
                toastText(this, R.string.invalid_emp_mpt)
                return null
            }
        }
    }

    private fun readEMP(emp: Encrypted): Password? {
        val empSK = MasterPasswordService.generateEncMasterPasswdSKForExport(this)
        val masterPassword =
            SecretService.decryptPassword(empSK, emp)
        if (!masterPassword.isValid()) {
            toastText(this, R.string.invalid_emp)
            return null
        }

        return masterPassword
    }

    private fun readMPT(mpt: Encrypted): Password? {
        if (!PreferenceService.isPresent(
                PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
                this
            )
        ) {
            toastText(this, R.string.no_mpt_present)
            return null
        }
        // decrypt obliviously encrypted master password token
        val encMasterPasswordTokenKey = PreferenceService.getEncrypted(
            PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
            this
        )
        encMasterPasswordTokenKey?.let {
            val masterPasswordTokenSK =
                SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MP_TOKEN, this)
            val masterPasswordTokenKey =
                SecretService.decryptKey(masterPasswordTokenSK, encMasterPasswordTokenKey)
            val salt = SaltService.getSalt(this)
            val cipherAlgorithm = SecretService.getCipherAlgorithm(this)

            val mptSK =
                SecretService.generateStrongSecretKey(masterPasswordTokenKey, salt, cipherAlgorithm)

            val masterPassword =
                SecretService.decryptPassword(mptSK, mpt)
            if (masterPassword.isValid()) {
                return masterPassword
            }

        }
        toastText(this, R.string.invalid_mpt)
        return null
    }

    private fun getMaxLoginAttempts(): Int {
        return PreferenceService.getAsInt(PREF_MAX_LOGIN_ATTEMPTS, this)
    }


    private fun dropAndLogoutVault() {
        DropVaultUseCase.dropVaultData(this)
        Session.logout()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}