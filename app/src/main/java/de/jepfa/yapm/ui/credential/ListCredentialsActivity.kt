package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
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
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.ui.changelogin.ChangeMasterPasswordActivity
import de.jepfa.yapm.ui.changelogin.ChangePinActivity
import de.jepfa.yapm.ui.exportvault.ExportVaultActivity
import de.jepfa.yapm.usecase.*
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory


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

        if (Session.isDenied()) {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.menu_lock_items -> {
                LockVaultUseCase.execute(this)
                refreshMenuLockItem(item)
                return true
            }
            R.id.menu_logout -> {
                return LogoutUseCase.execute(this)
            }
            R.id.delete_stored_masterpasswd -> {
                return DeleteStoredMasterPasswordUseCase.execute(this)
            }
            R.id.generate_encrypted_masterpasswd -> {
                return GenerateMasterPasswordTokenUseCase.execute(this)
            }
            R.id.export_plain_masterpasswd -> {
                return ExportPlainMasterPasswordUseCase.execute(this)
            }
            R.id.export_masterkey -> {
                return ExportEncMasterKeyUseCase.execute(this)
            }
            R.id.export_vault -> {
                val intent = Intent(this, ExportVaultActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.change_pin -> {
                val intent = Intent(this, ChangePinActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.change_master_password -> {
                val intent = Intent(this, ChangeMasterPasswordActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.drop_vault -> {
                AlertDialog.Builder(this)
                        .setTitle("Drop vault")
                        .setMessage("You are going to delete ALL your credentials and login data.")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                            DropVaultUseCase.execute(this)
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()

                return true
            }
            R.id.menu_help -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://jepfa.de"))
                startActivity(browserIntent)
                return true
            }

            R.id.menu_about -> {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                val icon: Drawable = getApplicationInfo().loadIcon(getPackageManager())
                val message = getString(R.string.app_name) + ", Version " + getVersionName() +
                        System.lineSeparator() + " \u00A9 Jens Pfahl 2021"
                builder.setTitle(R.string.title_about_the_app)
                        .setMessage(message)
                        .setIcon(icon)
                        .show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
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

    private fun refreshMenuLockItem(lockItem: MenuItem) {
        val secret = SecretChecker.getOrAskForSecret(this)
        if (secret.isDenied()) {
            lockItem.setIcon(R.drawable.ic_lock_outline_white_24dp)
        } else {
            lockItem.setIcon(R.drawable.ic_lock_open_white_24dp)
        }
    }

    fun deleteCredential(credential: EncCredential) {
        credentialViewModel.delete(credential)
    }

    private fun getVersionName(): String {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            return pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return "?"
    }

}