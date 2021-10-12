package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD
import de.jepfa.yapm.service.PreferenceService.PREF_CREDENTIAL_SORT_ORDER
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_CREDENTIAL_IDS
import de.jepfa.yapm.service.autofill.CurrentCredentialHolder
import de.jepfa.yapm.service.autofill.ResponseFiller
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.MasterPasswordService.storeMasterPassword
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.changelogin.ChangeMasterPasswordActivity
import de.jepfa.yapm.ui.changelogin.ChangePinActivity
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.ui.exportvault.ExportVaultActivity
import de.jepfa.yapm.ui.importread.ImportCredentialActivity
import de.jepfa.yapm.ui.importread.VerifyActivity
import de.jepfa.yapm.ui.label.ListLabelsActivity
import de.jepfa.yapm.ui.settings.SettingsActivity
import de.jepfa.yapm.usecase.*
import de.jepfa.yapm.util.*
import java.util.*
import kotlin.collections.ArrayList

class ListCredentialsActivity : SecureActivity(), NavigationView.OnNavigationItemSelectedListener  {

    var assistStructure: AssistStructure? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    val newOrUpdateCredentialActivityRequestCode = 1

    private var listCredentialAdapter: ListCredentialAdapter? = null
    private var credentialCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_credentials)
        val toolbar: Toolbar = findViewById(R.id.list_credentials_toolbar)
        setSupportActionBar(toolbar)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        listCredentialAdapter = ListCredentialAdapter(this)
        recyclerView.adapter = listCredentialAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        CurrentCredentialHolder.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assistStructure = intent.getParcelableExtra(AutofillManager.EXTRA_ASSIST_STRUCTURE)
        }

        refreshCredentials()

        labelViewModel.allLabels.observe(this, { labels ->
            masterSecretKey?.let{ key ->
                LabelService.initLabels(key, labels.toSet())
            }
        })

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@ListCredentialsActivity, EditCredentialActivity::class.java)
            startActivityForResult(intent, newOrUpdateCredentialActivityRequestCode)
        }

        fab.setOnLongClickListener {
            it.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        view.x = event.getRawX() - (view.getWidth() / 2)
                        view.y= event.getRawY() - (view.getHeight())
                    }
                    MotionEvent.ACTION_UP -> view.setOnTouchListener(null)
                    else -> {
                    }
                }
                true
            }
            true
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_drawer_view)

        navigationView.setNavigationItemSelectedListener(this)
        refreshMenuMasterPasswordItem(navigationView.menu)
        refreshMenuDebugItem(navigationView.menu)

        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onResume() {
        super.onResume()
        listCredentialAdapter?.notifyDataSetChanged()
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
        val searchView = MenuItemCompat.getActionView(searchItem) as SearchView

        val searchPlate = searchView.findViewById(R.id.search_src_text) as EditText
        searchPlate.hint = getString(R.string.search)
        val searchPlateView: View =
            searchView.findViewById(R.id.search_plate)
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
                listCredentialAdapter?.filter?.filter(s)

                return false
            }
        })

        val searchManager =
                getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))


        refreshMenuLockItem(menu.findItem(R.id.menu_lock_items))
        refreshMenuFiltersItem(menu.findItem(R.id.menu_filter))
        refreshMenuShowIdsItem(menu.findItem(R.id.menu_show_ids))

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
            R.id.menu_filter -> {
                val inflater: LayoutInflater = layoutInflater
                val labelsView: View = inflater.inflate(R.layout.content_dynamic_labels_list, null)
                val labelsContainer: LinearLayout = labelsView.findViewById(R.id.dynamic_labels)

                val allLabels = LabelService.getAllLabels()
                val allChips = ArrayList<Chip>(allLabels.size)

                allLabels.forEachIndexed { idx, label ->
                    val chip = createAndAddLabelChip(label, labelsContainer, this)
                    chip.isClickable = true
                    chip.isCheckable = true
                    chip.isChecked = LabelFilter.isFilterFor(label)
                    allChips.add(chip)
                }

                val dialog = AlertDialog.Builder(this)
                    .setTitle(getString(R.string.filter))
                    .setIcon(R.drawable.ic_baseline_filter_list_24)
                    .setView(labelsView)
                    .setNeutralButton(getString(R.string.deselect_all), null)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()

                dialog.setOnShowListener {
                    val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    buttonPositive.setOnClickListener {
                        LabelFilter.unsetAllFilters()
                        for (i in 0 until allChips.size) {
                            val checked = allChips[i].isChecked
                            val labelId = allLabels[i].labelId
                            if (labelId != null) {
                                val label = LabelService.lookupByLabelId(labelId)
                                if (label != null) {
                                    if (checked) {
                                        LabelFilter.setFilterFor(label)
                                    } else {
                                        LabelFilter.unsetFilterFor(label)
                                    }
                                }
                            }
                        }

                        listCredentialAdapter?.filter?.filter("")
                        refreshMenuFiltersItem(item)
                        dialog.dismiss()
                    }

                    val buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    buttonNegative.setOnClickListener {
                        dialog.dismiss()
                    }

                    val buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    buttonNeutral.setOnClickListener {
                        allChips.forEach { it.isChecked = false }
                    }
                }

                dialog.show()

                return true
            }
            R.id.menu_sort_order -> {
                val prefSortOrder = getPrefSortOrder()
                val listItems = CredentialSortOrder.values().map { getString(it.labelId) }.toTypedArray()

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_baseline_sort_24)
                    .setTitle(R.string.sort_order)
                    .setSingleChoiceItems(listItems, prefSortOrder.ordinal) { dialogInterface, i ->
                        dialogInterface.dismiss()

                        val newSortOrder = CredentialSortOrder.values()[i]
                        PreferenceService.putString(PREF_CREDENTIAL_SORT_ORDER, newSortOrder.name, this)
                        refreshCredentials()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()

                return true
            }
            R.id.menu_show_ids -> {
                PreferenceService.toggleBoolean(PREF_SHOW_CREDENTIAL_IDS, this)
                refreshMenuShowIdsItem(item)
                refreshCredentials()
                return true
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
        LabelService.clearAll()
        listCredentialAdapter?.notifyDataSetChanged()
    }

    fun shouldPushBackAutoFill() : Boolean {
        return assistStructure != null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun pushBackAutofill(credential: EncCredential, deobfuscationKey: Key?) {
        val structure = assistStructure
        if (structure != null) {
            CurrentCredentialHolder.update(credential, deobfuscationKey)
            val replyIntent = Intent().apply {
                val fillResponse = ResponseFiller.createFillResponse(
                    structure,
                    allowCreateAuthentication = false,
                    applicationContext
                )
                putExtra(EXTRA_AUTHENTICATION_RESULT, fillResponse)
            }
            assistStructure = null
            setResult(Activity.RESULT_OK, replyIntent)
            finish()

        }

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
                            toastText(this, R.string.masterpassword_stored)
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()

                    return true
                } else {
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
                        toastText(this, R.string.masterpassword_removed)
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()

                return true
            }
            R.id.generate_encrypted_masterpasswd -> {
                return GenerateMasterPasswordTokenUseCase.execute(this)
            }
            R.id.export_encrypted_masterpasswd -> {
                val encMasterPasswd = Session.getEncMasterPasswd()
                if (encMasterPasswd != null) {
                    ExportEncMasterPasswordUseCase.execute(encMasterPasswd, false, this)
                    return true
                } else {
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
            R.id.import_credential -> {
                val intent = Intent(this, ImportCredentialActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.test_verify_qr_code_or_nfc_tag -> {
                val intent = Intent(this, VerifyActivity::class.java)
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
            R.id.show_vault_info -> {
                val labelCount = LabelService.getAllLabels().size
                ShowVaultInfoUseCase.execute(this, credentialCount, labelCount)

                return true
            }
            R.id.drop_vault -> {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.title_drop_vault))
                    .setMessage(getString(R.string.message_drop_vault))
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

            R.id.menu_labels -> {
                val intent = Intent(this, ListLabelsActivity::class.java)
                startActivity(intent)
                return true
            }

            R.id.menu_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }

            R.id.menu_about -> {
                ShowInfoUseCase.execute(this)
                return true
            }
            R.id.menu_debug -> {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                val icon: Drawable = applicationInfo.loadIcon(packageManager)
                val message = DebugInfo.getDebugInfo(this)
                builder.setTitle(R.string.debug)
                    .setMessage(message)
                    .setIcon(icon)
                    .show()
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun refreshCredentials() {
        credentialViewModel.allCredentials.observe(this, { credentials ->
            credentials?.let { credentials ->
                credentialCount = credentials.size
                var sortedCredentials = credentials

                masterSecretKey?.let { key ->

                    credentials.forEach { LabelService.updateLabelsForCredential(key, it) }

                    when (getPrefSortOrder()) {
                        CredentialSortOrder.CREDENTIAL_NAME_ASC -> {
                            sortedCredentials = credentials
                                .sortedBy {
                                    SecretService.decryptCommonString(key, it.name).toLowerCase(Locale.ROOT)
                                }
                        }
                        CredentialSortOrder.CREDENTIAL_NAME_DESC -> {
                            sortedCredentials = credentials
                                .sortedBy {
                                    SecretService.decryptCommonString(key, it.name).toLowerCase(Locale.ROOT)
                                }
                                .reversed()
                        }
                        CredentialSortOrder.RECENTLY_MODIFIED -> {
                            sortedCredentials = credentials
                                .sortedBy { it.modifyTimestamp }
                                .reversed()
                        }
                        CredentialSortOrder.CREDENTIAL_IDENTIFIER -> {
                            sortedCredentials = credentials
                                .sortedBy { it.id }
                        }

                    }
                }

                listCredentialAdapter?.submitOriginList(sortedCredentials)
                listCredentialAdapter?.filter?.filter("")

            }
        })
    }

    private fun refreshMenuMasterPasswordItem(menu: Menu) {
        val storedMasterPasswdPresent = PreferenceService.isPresent(
            DATA_ENCRYPTED_MASTER_PASSWORD,
            this
        )

        val storeMasterPasswdItem: MenuItem = menu.findItem(R.id.store_masterpasswd)
        storeMasterPasswdItem.isVisible = !storedMasterPasswdPresent

        val deleteMasterPasswdItem: MenuItem = menu.findItem(R.id.delete_stored_masterpasswd)
        deleteMasterPasswdItem.isVisible = storedMasterPasswdPresent
    }

    private fun refreshMenuDebugItem(menu: Menu) {
        val debugItem: MenuItem = menu.findItem(R.id.menu_debug)
        debugItem.isVisible = DebugInfo.isDebug

    }

    private fun refreshMenuFiltersItem(item: MenuItem) {
        val hasFilters = LabelFilter.hasFilters()
        item.isChecked = hasFilters
        if (hasFilters) {
            item.setIcon(R.drawable.ic_baseline_filter_list_with_with_dot_24dp)
        }
        else {
            item.setIcon(R.drawable.ic_baseline_filter_list_24_white)
        }
    }

    private fun refreshMenuShowIdsItem(item: MenuItem) {
        val showIds = PreferenceService.getAsBool(PREF_SHOW_CREDENTIAL_IDS, this)
        item.isChecked = showIds
    }

    private fun getPrefSortOrder(): CredentialSortOrder {
        val sortOrderAsString = PreferenceService.getAsString(PREF_CREDENTIAL_SORT_ORDER, this)
        if (sortOrderAsString != null) {
            return CredentialSortOrder.valueOf(sortOrderAsString)
        }
        return CredentialSortOrder.DEFAULT
    }

    fun deleteCredential(credential: EncCredential) {
        credentialViewModel.delete(credential)
    }

}

