package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Filterable
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.encrypt.SecretService.encryptCommonString
import de.jepfa.yapm.service.encrypt.SecretService.encryptPassword
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.ui.exportvault.ExportVaultActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


class ListCredentialsActivity : SecureActivity() {

    val newOrUpdateCredentialActivityRequestCode = 1

    private lateinit var credentialListAdapter: CredentialListAdapter

    private val credentialViewModel: CredentialViewModel by viewModels {
        CredentialViewModelFactory((application as YapmApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_credentials)
        setSupportActionBar(findViewById(R.id.toolbar))

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        credentialListAdapter = CredentialListAdapter(this)
        recyclerView.adapter = credentialListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        credentialViewModel.allCredentials.observe(this, Observer { credentials ->
            credentials?.let { credentialListAdapter.submitOriginList(it) }
        })

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@ListCredentialsActivity, NewOrChangeCredentialActivity::class.java)
            startActivityForResult(intent, newOrUpdateCredentialActivityRequestCode)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        if (Secret.isDenied()) {
            return false
        }

        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem: MenuItem = menu.findItem(R.id.action_search)
        if (searchItem != null) {
            val searchView = MenuItemCompat.getActionView(searchItem) as SearchView
            searchView.setOnCloseListener(object : SearchView.OnCloseListener {
                override fun onClose(): Boolean {
                    return true
                }
            })

            val searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
            searchPlate.hint = "Search"
            val searchPlateView: View =
                    searchView.findViewById(androidx.appcompat.R.id.search_plate)
            searchPlateView.setBackgroundColor(
                    ContextCompat.getColor(
                            this,
                            android.R.color.transparent
                    )
            )

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchItem.collapseActionView()
                    return false
                }

                override fun onQueryTextChange(s: String?): Boolean {
                    val filterable: Filterable = credentialListAdapter
                    if (filterable != null) {
                        filterable.filter.filter(s)
                    }
                    return false
                }
            })

            val searchManager =
                    getSystemService(Context.SEARCH_SERVICE) as SearchManager
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        }

        val lockItem = menu.findItem(R.id.menu_lock_items)
        refreshMenuLockItem(lockItem)

        val deleteMasterPasswdItem: MenuItem = menu.findItem(R.id.delete_stored_masterpasswd)
        if (deleteMasterPasswdItem != null) {
            val encMasterPasswd = PreferenceUtil.get(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, this)
            deleteMasterPasswdItem.setVisible(encMasterPasswd != null)
        }

        return super.onCreateOptionsMenu(menu)
    }


    protected fun refreshMenuLockItem(lockItem: MenuItem) {
        val secret = SecretChecker.getOrAskForSecret(this)
        if (secret.isDenied()) {
            lockItem.setIcon(R.drawable.ic_lock_outline_white_24dp)
        } else {
            lockItem.setIcon(R.drawable.ic_lock_open_white_24dp)
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.menu_lock_items -> {

                Secret.lock()
                closeOverlayDialogs()
                refreshMenuLockItem(item)
                finishAffinity()
                SecretChecker.getOrAskForSecret(this)
                
                return true
            }
            R.id.menu_logout -> {
                Secret.logout()

                closeOverlayDialogs()

                finishAndRemoveTask()
                finishAffinity()

                return true
            }
            R.id.delete_stored_masterpasswd -> {
                if (!Secret.isDenied()) {
                    PreferenceUtil.delete(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, this)
                    Secret.logout()
                }
                closeOverlayDialogs()

                SecretChecker.getOrAskForSecret(this)

                return true
            }
            R.id.export_masterpasswd -> {

                val key = masterSecretKey
                val encMasterPasswd = Secret.getEncMasterPasswd()
                if (key != null && encMasterPasswd != null) {
                    val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)

                    val encHead = encryptCommonString(tempKey, "Your master password")
                    val encSub = encryptCommonString(tempKey, "Take this code in your wallet to scan for login.")
                    val encQrc = encMasterPasswd

                    val intent = Intent(this, QrCodeActivity::class.java)
                    intent.putExtra(QrCodeActivity.EXTRA_HEADLINE, encHead.toBase64String())
                    intent.putExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub.toBase64String())
                    intent.putExtra(QrCodeActivity.EXTRA_QRCODE, encQrc.toBase64String())

                    startActivity(intent)

                }

                return true
            }
            R.id.export_masterkey -> {

                val encStoredMasterKey = PreferenceUtil.getEncrypted(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, this)
                val key = masterSecretKey
                if (key != null && encStoredMasterKey != null) {

                    val mkKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
                    val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)
                    val encMasterKeyBase64 = SecretService.decryptEncrypted(mkKey, encStoredMasterKey).toBase64String()

                    val encHead = encryptCommonString(tempKey, "Encrypted master key")
                    val encSub = encryptCommonString(tempKey, "Store this at a safe place. Future backups don't need to include that master key.")
                    val encQrc = encryptPassword(tempKey, Password(encMasterKeyBase64))

                    val intent = Intent(this, QrCodeActivity::class.java)
                    intent.putExtra(QrCodeActivity.EXTRA_HEADLINE, encHead.toBase64String())
                    intent.putExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub.toBase64String())
                    intent.putExtra(QrCodeActivity.EXTRA_QRCODE, encQrc.toBase64String())

                    startActivity(intent)

                }

                return true
            }
            R.id.export_vault -> {
                val intent = Intent(this, ExportVaultActivity::class.java)
                startActivity(intent)

                return true
            }
            R.id.drop_vault -> {

                AlertDialog.Builder(this)
                        .setTitle("Drop vault")
                        .setMessage("You are going to delete ALL your credentials and login data.")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                            Secret.logout()
                            closeOverlayDialogs()
                            dropVault()
                            finishAffinity()
                            SecretChecker.getOrAskForSecret(this) // restart app
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()

                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun dropVault() {

        PreferenceUtil.delete(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, this)
        PreferenceUtil.delete(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, this)
        PreferenceUtil.delete(PreferenceUtil.PREF_SALT, this)
        CoroutineScope(Dispatchers.IO).launch {
            getApp().database?.clearAllTables()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == newOrUpdateCredentialActivityRequestCode && resultCode == Activity.RESULT_OK) {
            data?.let {

                var id: Int? = null
                val idExtra = it.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)
                if (idExtra != -1) {
                    id = idExtra
                }
                val nameBase64 = it.getStringExtra(EncCredential.EXTRA_CREDENTIAL_NAME)
                val additionalInfoBase64 = it.getStringExtra(EncCredential.EXTRA_CREDENTIAL_ADDITIONAL_INFO)
                val passwordBase64 = it.getStringExtra(EncCredential.EXTRA_CREDENTIAL_PASSWORD)

                val encName = Encrypted.fromBase64String(nameBase64)
                val encAdditionalInfo = Encrypted.fromBase64String(additionalInfoBase64)
                val encPassword = Encrypted.fromBase64String(passwordBase64)

                val credential = EncCredential(id, encName, encAdditionalInfo, encPassword)
                if (credential.isPersistent()) {
                    credentialViewModel.update(credential)
                }
                else {
                    credentialViewModel.insert(credential)
                }
            }
        }

    }

    override fun lock() {
        recreate()
    }

    fun deleteCredential(credential: EncCredential) {
        credentialViewModel.delete(credential)
    }

}