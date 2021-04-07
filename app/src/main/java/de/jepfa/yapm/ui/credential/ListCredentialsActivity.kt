package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Filterable
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.ui.changelogin.ChangeMasterPasswordActivity
import de.jepfa.yapm.ui.changelogin.ChangePinActivity
import de.jepfa.yapm.ui.exportvault.ExportVaultActivity
import de.jepfa.yapm.ui.settings.SettingsActivity
import de.jepfa.yapm.usecase.*
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.MasterPasswordService.storeMasterPassword
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.util.*
import de.jepfa.yapm.util.PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory


class ListCredentialsActivity : SecureActivity(), NavigationView.OnNavigationItemSelectedListener  {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    val newOrUpdateCredentialActivityRequestCode = 1

    private lateinit var credentialListAdapter: CredentialListAdapter

    private val credentialViewModel: CredentialViewModel by viewModels {
        CredentialViewModelFactory((application as YapmApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_credentials)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        credentialListAdapter = CredentialListAdapter(this)
        recyclerView.adapter = credentialListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        credentialViewModel.allCredentials.observe(this, Observer { credentials ->
            credentials?.let {
                val key = masterSecretKey
                if (key != null) {
                    val sorted = it.sortedBy {
                        SecretService.decryptCommonString(key, it.name).toLowerCase()
                    }
                    credentialListAdapter.submitOriginList(sorted)
                }
                else {
                    credentialListAdapter.submitOriginList(credentials)

                }
            }
        })

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@ListCredentialsActivity, EditCredentialActivity::class.java)
            startActivityForResult(intent, newOrUpdateCredentialActivityRequestCode)
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_drawer_view)

        navigationView.setNavigationItemSelectedListener(this)
        refreshMenuMasterPasswordItem(navigationView.menu)

        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
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

        refreshMenuLockItem(menu.findItem(R.id.menu_lock_items))

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (toggle.onOptionsItemSelected(item)) {
            return true
        }

        return when (item.itemId) {
            R.id.menu_lock_items -> {
                LockVaultUseCase.execute(this)
                refreshMenuLockItem(item)
                return true
            }
            R.id.menu_logout -> {
                return LogoutUseCase.execute(this)
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == newOrUpdateCredentialActivityRequestCode && resultCode == Activity.RESULT_OK) {
            data?.let {

                val credential = EncCredential.fromIntent(it)
                if (credential.isPersistent()) {
                    credentialViewModel.update(credential)
                }
                else {
                    credentialViewModel.insert(credential)
                }
            }
        }

    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        else {
            super.onBackPressed()
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        drawerLayout.closeDrawer(GravityCompat.START)

        when (item.itemId) {

            R.id.store_masterpasswd -> {
                val masterPasswd = getMasterPasswordFromSession()
                if (masterPasswd != null) {

                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.store_masterpasswd))
                        .setMessage(getString(R.string.store_masterpasswd_confirmation))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                            storeMasterPassword(masterPasswd, this)
                            refreshMenuMasterPasswordItem(navigationView.menu)
                            masterPasswd.clear()
                            Toast.makeText(this, "Master password stored on device", Toast.LENGTH_LONG).show()
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()

                    return true
                }
                else {
                    return false
                }
            }
            R.id.delete_stored_masterpasswd -> {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_stored_masterpasswd))
                    .setMessage(getString(R.string.delete_stored_masterpasswd_confirmation))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                        RemoveStoredMasterPasswordUseCase.execute(this)
                        refreshMenuMasterPasswordItem(navigationView.menu)
                        Toast.makeText(this, "Stored master password removed on device", Toast.LENGTH_LONG).show()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()

                return true
            }
            R.id.generate_encrypted_masterpasswd -> {
                return GenerateMasterPasswordTokenUseCase.execute(this)
            }
            R.id.export_plain_masterpasswd -> {
                val encMasterPasswd = Session.getEncMasterPasswd()
                if (encMasterPasswd != null) {
                    ExportPlainMasterPasswordUseCase.execute(encMasterPasswd, false, this)
                    return true
                }
                else {
                    return false
                }
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
            R.id.test_copypaste_password -> {
                ClipboardUtil.copyTestPasteConsumer(this)
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
                val browserIntent = Intent(Intent.ACTION_VIEW, Constants.HOMEPAGE)
                startActivity(browserIntent)
                return true
            }
            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
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

        return true
    }

    private fun refreshMenuMasterPasswordItem(menu: Menu) {
        val storedMasterPasswdPresent = PreferenceUtil.isPresent(PREF_ENCRYPTED_MASTER_PASSWORD, this)

        val storeMasterPasswdItem: MenuItem = menu.findItem(R.id.store_masterpasswd)
        if (storeMasterPasswdItem != null) {
            storeMasterPasswdItem.setVisible(!storedMasterPasswdPresent)
        }
        val deleteMasterPasswdItem: MenuItem = menu.findItem(R.id.delete_stored_masterpasswd)
        if (deleteMasterPasswdItem != null) {
            deleteMasterPasswdItem.setVisible(storedMasterPasswdPresent)
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

