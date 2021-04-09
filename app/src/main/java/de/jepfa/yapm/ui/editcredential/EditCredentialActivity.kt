package de.jepfa.yapm.ui.editcredential

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import com.google.android.material.tabs.TabLayout
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secretgenerator.*
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_PENTIUM
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.BRUTEFORCE_ATTEMPTS_SUPERCOMP
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.PasswordColorizer.spannableString
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.PREF_USE_PREUDO_PHRASE
import de.jepfa.yapm.util.PreferenceUtil.PREF_WITH_DIGITS
import de.jepfa.yapm.util.PreferenceUtil.PREF_WITH_SPECIAL_CHARS
import de.jepfa.yapm.util.PreferenceUtil.PREF_WITH_UPPER_CASE
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory
import java.util.*


class EditCredentialActivity : SecureActivity() {

    var currentId: Int? = null
    lateinit var current: EncCredential

    private val credentialViewModel: CredentialViewModel by viewModels {
        CredentialViewModelFactory((application as YapmApp).repository)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return
        }
        setContentView(R.layout.activity_edit_credential)

        val idExtra = intent.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)
        if (idExtra == -1) {
            setTitle(R.string.title_new_credential)
        }
        else {
            currentId = idExtra
            setTitle(R.string.title_update_credential)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun lock() {
        //generatedPassword.clear()
        recreate()
    }

    fun isUpdate(): Boolean {
        return currentId != null
    }

    fun load(): LiveData<EncCredential> {
        return credentialViewModel.getById(currentId!!)
    }

    fun reply() {
        val replyIntent = Intent()
        current.applyExtras(replyIntent)

        setResult(Activity.RESULT_OK, replyIntent)
        finish()
    }

}