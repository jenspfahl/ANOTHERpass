package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.*
import android.view.autofill.AutofillManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
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
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_ENCRYPTED_MASTER_PASSWORD
import de.jepfa.yapm.service.PreferenceService.PREF_CREDENTIAL_SORT_ORDER
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_CREDENTIAL_IDS
import de.jepfa.yapm.service.PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD
import de.jepfa.yapm.service.PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_RELOAD
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelFilter.WITH_NO_LABELS_ID
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.notification.ReminderService
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.MasterPasswordService.storeMasterPassword
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.changelogin.ChangeMasterPasswordActivity
import de.jepfa.yapm.ui.changelogin.ChangePinActivity
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.ui.exportvault.ExportVaultActivity
import de.jepfa.yapm.ui.importread.ImportCredentialActivity
import de.jepfa.yapm.ui.importread.VerifyActivity
import de.jepfa.yapm.ui.importvault.ImportVaultActivity
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.ui.label.ListLabelsActivity
import de.jepfa.yapm.ui.settings.SettingsActivity
import de.jepfa.yapm.usecase.app.ShowInfoUseCase
import de.jepfa.yapm.usecase.session.LogoutUseCase
import de.jepfa.yapm.usecase.vault.DropVaultUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.usecase.vault.ShowVaultInfoUseCase
import de.jepfa.yapm.util.*
import java.util.*
import kotlin.collections.ArrayList
import androidx.recyclerview.widget.DividerItemDecoration
import de.jepfa.yapm.service.PreferenceService.PREF_AUTOFILL_SUGGEST_CREDENTIALS
import de.jepfa.yapm.service.autofill.ResponseFiller
import de.jepfa.yapm.service.autofill.ResponseFiller.ACTION_DELIMITER
import de.jepfa.yapm.ui.changelogin.ChangeEncryptionActivity
import de.jepfa.yapm.ui.importcredentials.ImportCredentialsActivity
import de.jepfa.yapm.usecase.secret.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


/**
 * This is the main activity
 */
class ListCredentialsActivity : AutofillPushBackActivityBase(), NavigationView.OnNavigationItemSelectedListener  {

    private var searchItem: MenuItem? = null
    private var credentialsRecycleView: RecyclerView? = null
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private var resumeAutofillItem: MenuItem? = null

    val newOrUpdateCredentialActivityRequestCode = 1

    private var listCredentialAdapter: ListCredentialAdapter? = null
    private var credentialCount = 0

    private var jumpToUuid: UUID? = null
    private var jumpToItemPosition: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // session check would bring LoginActivity to front when action is invoked
        checkSession = false
        super.onCreate(savedInstanceState)
        intent?.action?.let { action ->
            Log.i("LST", "action=$action")
            if (action.startsWith(ResponseFiller.ACTION_CLOSE_VAULT)) {
                Session.lock()
                pushBackAutofill(allowCreateAuthentication = true)
                toastText(this, R.string.vault_locked)
                return
            }
            if (action.startsWith(ResponseFiller.ACTION_EXCLUDE_FROM_AUTOFILL)) {
                pushBackAutofill(ignoreCurrentApp = true)
                toastText(this, R.string.excluded_from_autofill)
                return
            }
            if (action.startsWith(ResponseFiller.ACTION_PAUSE_AUTOFILL)) {
                val pauseDurationInSec = PreferenceService.getAsString(PreferenceService.PREF_AUTOFILL_DEACTIVATION_DURATION, this)
                if (pauseDurationInSec != null) {
                    pauseAutofill(pauseDurationInSec)
                    return
                }
            }
        }
        // now lets check session
        checkSession = true
        SecretChecker.getOrAskForSecret(this)

        setContentView(R.layout.activity_list_credentials)
        val toolbar: Toolbar = findViewById(R.id.list_credentials_toolbar)
        setSupportActionBar(toolbar)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        listCredentialAdapter = ListCredentialAdapter(this)
        recyclerView.adapter = listCredentialAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        val showDividers = PreferenceService.getAsBool(PreferenceService.PREF_SHOW_DIVIDERS_IN_LIST, this)
        if (showDividers) {
            val dividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL
            )
            recyclerView.addItemDecoration(dividerItemDecoration)
        }

        listCredentialAdapter?.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val linearLayoutManager = credentialsRecycleView?.layoutManager as LinearLayoutManager

                if (jumpToUuid != null) {
                    val index = listCredentialAdapter?.currentList?.indexOfFirst { it.uid == jumpToUuid }
                    index?.let {
                        linearLayoutManager.scrollToPositionWithOffset(it, 10)
                    }
                } else {
                    jumpToItemPosition?.let {
                        linearLayoutManager.scrollToPositionWithOffset(it, 10)
                    }
                }
                jumpToItemPosition = null
                jumpToUuid = null

            }
        })

        credentialsRecycleView = recyclerView

        labelViewModel.allLabels.observe(this) { labels ->
            masterSecretKey?.let { key ->
                LabelService.defaultHolder.initLabels(key, labels.toSet())
                LabelFilter.initState(this, LabelService.defaultHolder.getAllLabels())
                refreshCredentials()
            }
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@ListCredentialsActivity, EditCredentialActivity::class.java)
            intent.action = this.intent.action
            intent.putExtras(this.intent) // forward all extras, especially needed for Autofill
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
        refreshRevokeMptItem(navigationView.menu)
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

        refreshMenuMasterPasswordItem(navigationView.menu)
        refreshRevokeMptItem(navigationView.menu)
        updateResumeAutfillMenuItem()

        val requestReload = PreferenceService.getAsBool(STATE_REQUEST_CREDENTIAL_LIST_RELOAD, applicationContext)
        val requestHardReload = PreferenceService.getAsBool(STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD, applicationContext)
        if (requestReload || requestHardReload) {
            PreferenceService.delete(STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD, applicationContext)
            PreferenceService.delete(STATE_REQUEST_CREDENTIAL_LIST_RELOAD, applicationContext)

            if (requestHardReload) {
                recreate()
            }
            else {
                listCredentialAdapter?.notifyDataSetChanged()
            }
        }

        val view: View = findViewById(R.id.content_list_credentials)

        var showed = ReminderService.showReminders(ReminderService.MasterPassword, view, this)
        if (!showed) {
            showed = ReminderService.showReminders(ReminderService.Vault, view, this)
        }
        if (!showed) {
            showed = ReminderService.showReminders(ReminderService.MasterKey, view, this)
        }
        if (!showed) {
            showed = ReminderService.showReminders(ReminderService.StoredMasterPassword, view, this)
        }
        if (!showed) {
            showed = ReminderService.showReminders(ReminderService.RefreshMasterPasswordToken, view, this)
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)
        Log.d("LST", "inflate menues")

        val searchItem: MenuItem = menu.findItem(R.id.action_search)
        this.searchItem = searchItem
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

        labelViewModel.allLabels.observe(this) { labels ->
            masterSecretKey?.let { key ->
                refreshMenuFiltersItem(menu.findItem(R.id.menu_filter))
            }
        }

        resumeAutofillItem = menu.findItem(R.id.menu_resume_autofill)
        updateResumeAutfillMenuItem()

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPostResume() {
        super.onPostResume()
        updateSearchFieldWithAutofillSuggestion()
    }
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        updateSearchFieldWithAutofillSuggestion()
        return super.onPrepareOptionsMenu(menu)
    }

    private fun updateSearchFieldWithAutofillSuggestion() {
        if (!Session.isDenied()) {
            intent?.action?.let { action ->
                Log.i("LST", "action2=$action")
                if (action.startsWith(ResponseFiller.ACTION_OPEN_VAULT)) {
                    val suggestCredentials =
                        PreferenceService.getAsBool(PREF_AUTOFILL_SUGGEST_CREDENTIALS, true, this)
                    if (suggestCredentials) {
                        val searchString = action.substringAfter(ACTION_DELIMITER).substringBeforeLast(ACTION_DELIMITER).lowercase()
                        if (searchString.isNotBlank()) {
                            searchItem?.let { searchItem ->
                                Log.i("LST", "update search text")

                                val searchView =
                                    MenuItemCompat.getActionView(searchItem) as SearchView
                                val searchPlate =
                                    searchView.findViewById(R.id.search_src_text) as EditText

                                searchView.setQuery("!$searchString", true)
                                searchItem.expandActionView()
                                searchPlate.text = SpannableStringBuilder("!$searchString")
                                searchPlate.selectAll()
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (toggle.onOptionsItemSelected(item)) {
            return true
        }

        return when (item.itemId) {
            R.id.menu_lock_items -> {
                if (shouldPushBackAutoFill()) {
                    Session.lock()
                    pushBackAutofill(allowCreateAuthentication = true)
                    return true
                }
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

                val allLabels = ArrayList<Label>()
                val noLabel = Label(WITH_NO_LABELS_ID, getString(R.string.no_label), "", null)
                allLabels.add(noLabel)
                allLabels.addAll(LabelService.defaultHolder.getAllLabels())
                val allChips = ArrayList<Chip>(allLabels.size)

                allLabels.forEachIndexed { _, label ->
                    val chip = createAndAddLabelChip(label, labelsContainer, thinner = false, this)
                    chip.isClickable = true
                    chip.isCheckable = true
                    chip.isChecked = LabelFilter.isFilterFor(label)
                    allChips.add(chip)
                }
                val builder = AlertDialog.Builder(this)
                val container = ScrollView(builder.context)
                container.addView(labelsView)

                val dialog = AlertDialog.Builder(this)
                    .setTitle(getString(R.string.filter))
                    .setIcon(R.drawable.ic_baseline_filter_list_24)
                    .setView(container)
                    .setNeutralButton(getString(R.string.select_none_all), null)
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
                                val label =
                                    if (labelId == WITH_NO_LABELS_ID) noLabel
                                    else LabelService.defaultHolder.lookupByLabelId(labelId)
                                if (label != null) {
                                    if (checked) {
                                        LabelFilter.setFilterFor(label)
                                    } else {
                                        LabelFilter.unsetFilterFor(label)
                                    }
                                }
                            }
                        }

                        LabelFilter.persistState(this)
                        filterAgain()
                        refreshMenuFiltersItem(item)
                        dialog.dismiss()
                    }

                    val buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    buttonNegative.setOnClickListener {
                        dialog.dismiss()
                    }

                    val buttonNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    buttonNeutral.setOnClickListener {
                        val nonSelected = allChips.none { it.isChecked }
                        allChips.forEach { it.isChecked = nonSelected }
                    }
                }

                dialog.show()

                return true
            }
            R.id.menu_sort_order -> {
                val prefSortOrder = getPrefSortOrder()
                val listItems = CredentialSortOrder.values().map { getString(it.labelId) }.toTypedArray()

                AlertDialog.Builder(this)
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
                listCredentialAdapter?.notifyDataSetChanged()
                return true
            }
            R.id.menu_resume_autofill -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ResponseFiller.isAutofillPaused(this)) {
                    ResponseFiller.resumeAutofill(this)
                    toastText(this, R.string.resume_paused_autofill_done)
                    item.isVisible = false
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data?.hasExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT) == true) {
            setResult(Activity.RESULT_OK, data)
            Log.i("CFS", "disable forwarded")
            finish()
            return
        }

        if (requestCode == SeedRandomGeneratorUseCase.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap?
            if (imageBitmap != null) {
                UseCaseBackgroundLauncher(SeedRandomGeneratorUseCase)
                    .launch(this, imageBitmap)
                    { output ->
                        if (output.success) {
                            val text = getString(
                                R.string.used_seed,
                                output.data
                            )
                            toastText(this, text)
                        }
                    }
            }
        }
        else if (requestCode == newOrUpdateCredentialActivityRequestCode && resultCode == Activity.RESULT_OK) {
            data?.let {

                val credential = EncCredential.fromIntent(it, createUuid = true)
                jumpToUuid = credential.uid

                if (credential.isPersistent()) {
                    credentialViewModel.update(credential, this)
                }
                else {
                    credentialViewModel.insert(credential, this)
                    if (shouldPushBackAutoFill()) {
                        pushBackAutofill()
                    } else {
                        // nothing
                    }
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
        LabelService.defaultHolder.clearAll()
        credentialsRecycleView?.let {
            it.post {
                listCredentialAdapter?.notifyDataSetChanged()
            }
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

    private fun refreshRevokeMptItem(menu: Menu) {
        val hasMpt = PreferenceService.isPresent(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, this)
        menu.findItem(R.id.revoke_masterpasswd_token).isVisible = hasMpt
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        drawerLayout.closeDrawer(GravityCompat.START)

        when (item.itemId) {

            R.id.store_masterpasswd -> {
                val masterPasswd = getMasterPasswordFromSession(this)
                if (masterPasswd != null) {

                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.store_masterpasswd))
                        .setMessage(getString(R.string.store_masterpasswd_confirmation))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                            storeMasterPassword(masterPasswd, this,
                                {
                                    refreshMenuMasterPasswordItem(navigationView.menu)
                                    masterPasswd.clear()
                                    toastText(this, R.string.masterpassword_stored)
                                },
                                {
                                    refreshMenuMasterPasswordItem(navigationView.menu)
                                    masterPasswd.clear()
                                    toastText(this, R.string.masterpassword_not_stored)
                                })
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
            R.id.generate_masterpasswd_token -> {
                GenerateMasterPasswordTokenUseCase.openDialog(this) {
                    refreshRevokeMptItem(navigationView.menu)
                }
                return true
            }
            R.id.revoke_masterpasswd_token -> {
                RevokeMasterPasswordTokenUseCase.openDialog(this) {
                    refreshRevokeMptItem(navigationView.menu)
                }

                return true
            }
            R.id.export_encrypted_masterpasswd -> {
                val encMasterPasswd = Session.getEncMasterPasswd()
                if (encMasterPasswd != null) {
                    ExportEncMasterPasswordUseCase.startUiFlow(this, encMasterPasswd, noSessionCheck = false)

                    return true
                } else {
                    return false
                }
            }
            R.id.export_masterkey -> {
                UseCaseBackgroundLauncher(ExportEncMasterKeyUseCase)
                    .launch(this, Unit)
                return true
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
            R.id.import_credentials_from_file -> {
                val intent = Intent(this, ImportCredentialsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.import_vault -> {
                val intent = Intent(this, ImportVaultActivity::class.java)
                intent.putExtra(ImportVaultActivity.EXTRA_MODE, ImportVaultActivity.EXTRA_MODE_OVERRIDE_IMPORT)
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
            R.id.change_master_key_and_encryption -> {
                val intent = Intent(this, ChangeEncryptionActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.seed_random_generator -> {
                SeedRandomGeneratorUseCase.openDialog(this)

                return true
            }
            R.id.show_vault_info -> {
                val labelCount = LabelService.defaultHolder.getAllLabels().size
                CoroutineScope(Dispatchers.Main).launch {
                    ShowVaultInfoUseCase.execute(ShowVaultInfoUseCase.Input(credentialCount, labelCount), this@ListCredentialsActivity)
                }

                return true
            }
            R.id.drop_vault -> {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.title_drop_vault))
                    .setMessage(getString(R.string.message_drop_vault))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                        DropVaultUseCase.doubleCheckDropVault(this)
                            {DropVaultUseCase.execute(this)}
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
        credentialViewModel.allCredentials.removeObservers(this)
        credentialViewModel.allCredentials.observe(this) { credentials ->
            credentials?.let { credentials ->
                credentialCount = credentials.size
                var sortedCredentials = credentials

                masterSecretKey?.let { key ->

                    credentials.forEach {
                        LabelService.defaultHolder.updateLabelsForCredential(
                            key,
                            it
                        )
                    }

                    when (getPrefSortOrder()) {
                        CredentialSortOrder.CREDENTIAL_NAME_ASC -> {
                            sortedCredentials = credentials
                                .sortedBy {
                                    SecretService.decryptCommonString(key, it.name)
                                        .toLowerCase(Locale.ROOT)
                                }
                        }
                        CredentialSortOrder.CREDENTIAL_NAME_DESC -> {
                            sortedCredentials = credentials
                                .sortedBy {
                                    SecretService.decryptCommonString(key, it.name)
                                        .toLowerCase(Locale.ROOT)
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
                filterAgain()

            }
        }
    }

    private fun filterAgain() {
        searchItem?.let { searchItem ->

            val searchView =
                MenuItemCompat.getActionView(searchItem) as SearchView

            if (searchView.query != null && searchView.query.isNotEmpty()) {
                // refresh filtering
                listCredentialAdapter?.filter?.filter(searchView.query)
            }
        }
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

    fun duplicateCredential(credential: EncCredential, key: SecretKeyHolder) {
        val name = SecretService.decryptCommonString(key, credential.name)
        val newName = getString(R.string.duplicate_of_name, name)
        val encNewName = SecretService.encryptCommonString(key, newName)
        val duplicated = credential.copy(id = null, uid = UUID.randomUUID(), name = encNewName)
        jumpToUuid = duplicated.uid
        credentialViewModel.insert(duplicated, this)
        toastText(this, R.string.credential_duplicated)
    }

    fun deleteCredential(credential: EncCredential) {
        jumpToItemPosition = (credentialsRecycleView?.layoutManager as LinearLayoutManager)
            .findFirstVisibleItemPosition()
        credentialViewModel.delete(credential)
        toastText(this, R.string.credential_deleted)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun pauseAutofill(
        pauseDurationInSec: String
    ) {
        val replyIntent = Intent().apply {
            val pauseResponse = ResponseFiller.createAutofillPauseResponse(this@ListCredentialsActivity, pauseDurationInSec.toLong())
            putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, pauseResponse)
        }
        setResult(Activity.RESULT_OK, replyIntent)
        Log.i("CFS", "disable clicked")
        val entries = resources.getStringArray(R.array.autofill_deactivation_duration_entries)
        val values = resources.getStringArray(R.array.autofill_deactivation_duration_values)
        values.indexOf(pauseDurationInSec).let {
            entries[it]?.let { entry ->
                toastText(this, getString(R.string.temp_deact_autofill_on, entry))
            }
        }
        finish()
    }

    private fun updateResumeAutfillMenuItem() {
        resumeAutofillItem?.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ResponseFiller.isAutofillPaused(this)
    }
}

