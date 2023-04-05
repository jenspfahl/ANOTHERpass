package de.jepfa.yapm.ui.credential

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.*
import android.view.autofill.AutofillManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.setPadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
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
import de.jepfa.yapm.service.PreferenceService.DATA_NAV_MENU_EXPORT_EXPANDED
import de.jepfa.yapm.service.PreferenceService.DATA_NAV_MENU_IMPORT_EXPANDED
import de.jepfa.yapm.service.PreferenceService.DATA_NAV_MENU_QUICK_ACCESS_EXPANDED
import de.jepfa.yapm.service.PreferenceService.DATA_NAV_MENU_VAULT_EXPANDED
import de.jepfa.yapm.service.PreferenceService.PREF_AUTOFILL_SUGGEST_CREDENTIALS
import de.jepfa.yapm.service.PreferenceService.PREF_CREDENTIAL_SORT_ORDER
import de.jepfa.yapm.service.PreferenceService.PREF_EXPIRED_CREDENTIALS_ON_TOP
import de.jepfa.yapm.service.PreferenceService.PREF_LABEL_FILTER_SINGLE_CHOICE
import de.jepfa.yapm.service.PreferenceService.PREF_NAV_MENU_ALWAYS_COLLAPSED
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_CREDENTIAL_IDS
import de.jepfa.yapm.service.PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD
import de.jepfa.yapm.service.PreferenceService.STATE_REQUEST_CREDENTIAL_LIST_RELOAD
import de.jepfa.yapm.service.autofill.ResponseFiller
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelFilter.WITH_NO_LABELS_ID
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.notification.NotificationService
import de.jepfa.yapm.service.notification.ReminderService
import de.jepfa.yapm.service.secret.MasterPasswordService.getMasterPasswordFromSession
import de.jepfa.yapm.service.secret.MasterPasswordService.storeMasterPassword
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.changelogin.ChangeEncryptionActivity
import de.jepfa.yapm.ui.changelogin.ChangeMasterPasswordActivity
import de.jepfa.yapm.ui.changelogin.ChangePinActivity
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.ui.exportvault.ExportPlainCredentialsActivity
import de.jepfa.yapm.ui.exportvault.ExportVaultActivity
import de.jepfa.yapm.ui.importcredentials.ImportCredentialsActivity
import de.jepfa.yapm.ui.importread.ImportCredentialActivity
import de.jepfa.yapm.ui.importread.VerifyActivity
import de.jepfa.yapm.ui.importvault.ImportVaultActivity
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.ui.label.ListLabelsActivity
import de.jepfa.yapm.ui.settings.SettingsActivity
import de.jepfa.yapm.usecase.app.ShowInfoUseCase
import de.jepfa.yapm.usecase.secret.*
import de.jepfa.yapm.usecase.session.LogoutUseCase
import de.jepfa.yapm.usecase.vault.DropVaultUseCase
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.usecase.vault.ShowVaultInfoUseCase
import de.jepfa.yapm.util.*
import de.jepfa.yapm.util.Constants.ACTION_DELIMITER
import de.jepfa.yapm.util.Constants.SEARCH_COMMAND_SEARCH_ID
import de.jepfa.yapm.util.Constants.SEARCH_COMMAND_SEARCH_IN_ALL
import de.jepfa.yapm.util.Constants.SEARCH_COMMAND_SEARCH_LABEL
import de.jepfa.yapm.util.Constants.SEARCH_COMMAND_SEARCH_UID
import de.jepfa.yapm.util.Constants.SEARCH_COMMAND_SEARCH_USER
import de.jepfa.yapm.util.Constants.SEARCH_COMMAND_SEARCH_WEBSITE
import de.jepfa.yapm.util.Constants.SEARCH_COMMAND_SHOW_EXPIRED
import de.jepfa.yapm.util.Constants.SEARCH_COMMAND_SHOW_EXPIRES
import de.jepfa.yapm.util.Constants.SEARCH_COMMAND_SHOW_VEILED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


/**
 * This is the main activity
 */
class ListCredentialsActivity : AutofillPushBackActivityBase(), NavigationView.OnNavigationItemSelectedListener  {

    private var navMenuQuickAccessVisible = true
    private var navMenuExportVisible = false
    private var navMenuImportVisible = false
    private var navMenuVaultVisible = false

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

        NotificationService.createNotificationChannel(
            this,
            NotificationService.CHANNEL_ID_SCHEDULED,
            getString(R.string.notification_channel_scheduled_title)
        )

        // session check would bring LoginActivity to front when action is invoked
        checkSession = false
        Log.i("LST", "onCreate")
        super.onCreate(savedInstanceState)
        intent?.action?.let { action ->
            Log.i("LST", "action=$action")
            if (action.startsWith(Constants.ACTION_CLOSE_VAULT)) {
                Session.lock()
                pushBackAutofill(allowCreateAuthentication = true)
                toastText(this, R.string.vault_locked)
                return
            }
            if (action.startsWith(Constants.ACTION_EXCLUDE_FROM_AUTOFILL)) {
                pushBackAutofill(ignoreCurrentApp = true)
                toastText(this, R.string.excluded_from_autofill)
                return
            }
            if (action.startsWith(Constants.ACTION_PAUSE_AUTOFILL)) {
                val pauseDurationInSec = PreferenceService.getAsString(PreferenceService.PREF_AUTOFILL_DEACTIVATION_DURATION, this)
                if (pauseDurationInSec != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pauseAutofill(pauseDurationInSec)
                    }
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
        drawerLayout.addDrawerListener(object: DrawerLayout.SimpleDrawerListener() {

            override fun onDrawerClosed(drawerView: View) {
                val navMenuAlwaysCollapsed = PreferenceService.getAsBool(
                    PREF_NAV_MENU_ALWAYS_COLLAPSED, false, this@ListCredentialsActivity)
                if (navMenuAlwaysCollapsed) {
                    setNavMenuCollapsed()
                    refreshNavigationMenu()
                }
            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onResume() {
        super.onResume()
        Log.i("LST", "onResume")
        updateSearchFieldWithAutofillSuggestion(intent)

        val navMenuAlwaysCollapsed = PreferenceService.getAsBool(
            PREF_NAV_MENU_ALWAYS_COLLAPSED, false, this@ListCredentialsActivity)
        if (navMenuAlwaysCollapsed) {
            setNavMenuCollapsed()
        }
        refreshNavigationMenu()

        updateResumeAutofillMenuItem()

        val requestReload = PreferenceService.getAsBool(STATE_REQUEST_CREDENTIAL_LIST_RELOAD, applicationContext)
        val requestHardReload = PreferenceService.getAsBool(STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD, applicationContext)
        if (requestReload || requestHardReload) {
            PreferenceService.delete(STATE_REQUEST_CREDENTIAL_LIST_ACTIVITY_RELOAD, applicationContext)
            PreferenceService.delete(STATE_REQUEST_CREDENTIAL_LIST_RELOAD, applicationContext)

            if (requestHardReload) {
                Log.d("LST", "hard reload")
                recreate()
            }
            else {
                listCredentialAdapter?.notifyDataSetChanged()
                Log.d("LST", "soft reload")
            }
        }

        val view: View = findViewById(R.id.content_list_credentials)

        // wait until ViewModel is fully loaded
        view.postDelayed({
            ReminderService.showNextReminder(view, this)
        }, 500L)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        inflateActionsMenu(menu, R.menu.menu_main)
        Log.i("LST", "onCreateOptionsMenu")

        val searchItem: MenuItem = menu.findItem(R.id.action_search)
        this.searchItem = searchItem
        val searchView = MenuItemCompat.getActionView(searchItem) as SearchView

        val searchPlate = searchView.findViewById(R.id.search_src_text) as AutoCompleteTextView
        searchPlate.threshold = 1

        searchPlate.hint = getString(R.string.search)
        val searchPlateView: View =
            searchView.findViewById(R.id.search_plate)
        searchPlateView.setBackgroundColor(
            ContextCompat.getColor(
                this,
                android.R.color.transparent
            )
        )

        val from = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1)
        val to = intArrayOf(R.id.search_suggestion)
        val cursorAdapter = androidx.cursoradapter.widget.SimpleCursorAdapter(this, R.layout.content_search_suggestion, null, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)
        val suggestions = listOf(
            SEARCH_COMMAND_SEARCH_IN_ALL,
            SEARCH_COMMAND_SEARCH_LABEL,
            SEARCH_COMMAND_SEARCH_ID,
            SEARCH_COMMAND_SEARCH_UID,
            SEARCH_COMMAND_SEARCH_USER,
            SEARCH_COMMAND_SEARCH_WEBSITE,
            SEARCH_COMMAND_SHOW_EXPIRED,
            SEARCH_COMMAND_SHOW_EXPIRES,
            SEARCH_COMMAND_SHOW_VEILED,
        )

        searchView.suggestionsAdapter = cursorAdapter

        searchView.setOnSuggestionListener(object: SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            @SuppressLint("Range")
            override fun onSuggestionClick(position: Int): Boolean {
                val cursor = searchView.suggestionsAdapter.getItem(position) as Cursor
                val selection = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))
                searchView.setQuery(selection, false)

                return true
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchItem.collapseActionView()
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                listCredentialAdapter?.filter?.filter(query)

                val cursor = MatrixCursor(arrayOf(BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1))
                query?.let {
                    suggestions.forEachIndexed { index, suggestion ->
                        if (suggestion.startsWith(it, ignoreCase = true)) {
                            cursor.addRow(arrayOf(index, suggestion))
                        }
                    }
                }

                cursorAdapter.changeCursor(cursor)

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
        updateResumeAutofillMenuItem()

        super.onCreateOptionsMenu(menu)

        // update navigation menu items (collapse or not)
        val navMenuAlwaysCollapsed = PreferenceService.getAsBool(PREF_NAV_MENU_ALWAYS_COLLAPSED, false, this)
        if (navMenuAlwaysCollapsed) {
            setNavMenuCollapsed()
        }
        else {
            navMenuQuickAccessVisible = PreferenceService.getAsBool(
                DATA_NAV_MENU_QUICK_ACCESS_EXPANDED, navMenuQuickAccessVisible, this
            )
            navMenuExportVisible = PreferenceService.getAsBool(
                DATA_NAV_MENU_EXPORT_EXPANDED, navMenuExportVisible, this
            )
            navMenuImportVisible = PreferenceService.getAsBool(
                DATA_NAV_MENU_IMPORT_EXPANDED, navMenuImportVisible, this
            )
            navMenuVaultVisible = PreferenceService.getAsBool(
                DATA_NAV_MENU_VAULT_EXPANDED, navMenuVaultVisible, this
            )
        }
        refreshNavigationMenu()


        return true
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i("LST", "onNewIntent: action=${intent?.action}")
        updateSearchFieldWithAutofillSuggestion(intent)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        Log.i("LST", "onPrepareOptionsMenu")
        updateSearchFieldWithAutofillSuggestion(intent)  //somehow important for autofill
        return super.onPrepareOptionsMenu(menu)
    }

    private fun updateSearchFieldWithAutofillSuggestion(intent: Intent?) {
        if (!Session.isDenied()) {
            intent?.action?.let { action ->
                Log.i("LST", "action2=$action")
                if (action.startsWith(Constants.ACTION_OPEN_VAULT_FOR_AUTOFILL)) {
                    val suggestCredentials =
                        PreferenceService.getAsBool(PREF_AUTOFILL_SUGGEST_CREDENTIALS, true, this)
                    if (suggestCredentials) {
                        val searchString = action.substringAfter(ACTION_DELIMITER).substringBeforeLast(ACTION_DELIMITER).lowercase()
                        if (searchString.isNotBlank()) {
                            startSearchFor("!$searchString")
                        }
                    }
                }
                else if (action.startsWith(Constants.ACTION_OPEN_VAULT_FOR_FILTERING)) {
                    val searchString = action.substringAfter(ACTION_DELIMITER).substringBeforeLast(ACTION_DELIMITER).lowercase()
                    Log.i("LST", "extracted search string=$searchString")

                    if (searchString.isNotBlank()) {
                        val success = startSearchFor("$searchString")
                        if (success) {
                            intent.action = null // one shot, don't filter again
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
                val container = LinearLayout(this)
                container.orientation = LinearLayout.VERTICAL
                val labelsContainer: LinearLayout = labelsView.findViewById(R.id.dynamic_labels)

                val multipleChoiceSwitch = SwitchCompat(this)
                multipleChoiceSwitch.text = getString(R.string.multiple_choice_selection)
                multipleChoiceSwitch.isChecked = !PreferenceService.getAsBool(PREF_LABEL_FILTER_SINGLE_CHOICE, this)
                multipleChoiceSwitch.switchPadding = 16
                multipleChoiceSwitch.setPadding(32)

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
                    chip.setOnClickListener {
                        if (!multipleChoiceSwitch.isChecked) {
                            allChips
                                .forEach { it.isChecked = false }
                            chip.isChecked = true
                        }
                    }
                    allChips.add(chip)
                }
                val builder = AlertDialog.Builder(this)
                val scrollView = ScrollView(builder.context)
                scrollView.addView(labelsView)
                container.addView(multipleChoiceSwitch)
                container.addView(scrollView)

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
                        PreferenceService.putBoolean(PREF_LABEL_FILTER_SINGLE_CHOICE, !multipleChoiceSwitch.isChecked, this)

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

                val view = LinearLayout(this)
                view.orientation = LinearLayout.HORIZONTAL
                view.setPadding(24)
                val checkBox = CheckBox(this)
                checkBox.isChecked = PreferenceService.getAsBool(PREF_EXPIRED_CREDENTIALS_ON_TOP, this)
                val desc = TextView(this)
                desc.text = getString(R.string.expired_credentials_on_top)
                view.addView(checkBox)
                view.addView(desc)

                var selectedSortOrder = prefSortOrder.ordinal
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.ic_baseline_sort_24)
                    .setTitle(R.string.sort_order)
                    .setSingleChoiceItems(listItems, prefSortOrder.ordinal) { _, i ->
                       selectedSortOrder = i
                    }
                    .setView(view)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()

                        val newSortOrder = CredentialSortOrder.values()[selectedSortOrder]
                        PreferenceService.putString(PREF_CREDENTIAL_SORT_ORDER, newSortOrder.name, this)
                        PreferenceService.putBoolean(PREF_EXPIRED_CREDENTIALS_ON_TOP, checkBox.isChecked, this)
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
        else if (requestCode == SecretChecker.loginRequestCode) {
            if (intent.getBooleanExtra(SecretChecker.fromAutofillOrNotification, false)) {
                Log.i("LST", "onActivityResult")
                updateSearchFieldWithAutofillSuggestion(intent)
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
        if (!navMenuQuickAccessVisible) {
            return
        }
        val hasMpt = PreferenceService.isPresent(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, this)
        menu.findItem(R.id.revoke_masterpasswd_token).isVisible = hasMpt
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        var needNavMenuUpdate = false
        when (item.itemId) {

            R.id.item_quick_access -> {
                navMenuQuickAccessVisible = !navMenuQuickAccessVisible
                PreferenceService.putBoolean(
                    DATA_NAV_MENU_QUICK_ACCESS_EXPANDED, navMenuQuickAccessVisible, this)
                needNavMenuUpdate = true
            }
            R.id.item_export -> {
                navMenuExportVisible = !navMenuExportVisible
                PreferenceService.putBoolean(
                    DATA_NAV_MENU_EXPORT_EXPANDED, navMenuExportVisible, this)
                needNavMenuUpdate = true
            }
            R.id.item_import -> {
                navMenuImportVisible = !navMenuImportVisible
                PreferenceService.putBoolean(
                    DATA_NAV_MENU_IMPORT_EXPANDED, navMenuImportVisible, this)
                needNavMenuUpdate = true
            }
            R.id.item_vault -> {
                navMenuVaultVisible = !navMenuVaultVisible
                PreferenceService.putBoolean(
                    DATA_NAV_MENU_VAULT_EXPANDED, navMenuVaultVisible, this)
                needNavMenuUpdate = true

            }
        }

        if (needNavMenuUpdate) {
            refreshNavigationMenu()
            return false
        }

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
            R.id.export_plain_credentials -> {
                val intent = Intent(this, ExportPlainCredentialsActivity::class.java)
                startActivity(intent)
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

    private fun refreshNavigationMenu() {
        navigationView.menu.clear()
        navigationView.inflateMenu(R.menu.menu_main_drawer)

        updateNavigationMenuVisibility(
            R.id.group_quick_access,
            R.id.item_quick_access,
            R.string.quick_access,
            navMenuQuickAccessVisible
        )
        updateNavigationMenuVisibility(
            R.id.group_export,
            R.id.item_export,
            R.string.action_export,
            navMenuExportVisible
        )
        updateNavigationMenuVisibility(
            R.id.group_import,
            R.id.item_import,
            R.string.import_read,
            navMenuImportVisible
        )
        updateNavigationMenuVisibility(
            R.id.group_vault,
            R.id.item_vault,
            R.string.vault,
            navMenuVaultVisible
        )

        refreshMenuMasterPasswordItem(navigationView.menu)
        refreshRevokeMptItem(navigationView.menu)
        refreshMenuDebugItem(navigationView.menu)

    }

    private fun refreshCredentials() {
        credentialViewModel.allCredentials.removeObservers(this)
        credentialViewModel.allCredentials.observe(this) { credentials ->
            credentials?.let { credentials ->
                credentialCount = credentials.size
                var sortedCredentials = credentials

                masterSecretKey?.let { key ->

                    credentialViewModel.clearExpiredCredentials()

                    credentials.forEach { credential ->
                        LabelService.defaultHolder.updateLabelsForCredential(
                            key,
                            credential
                        )

                        credentialViewModel.updateExpiredCredential(credential, key, this)
                    }

                    val expiredCredentialsOnTop = PreferenceService.getAsBool(PREF_EXPIRED_CREDENTIALS_ON_TOP, this)

                    when (getPrefSortOrder()) {
                        CredentialSortOrder.CREDENTIAL_NAME_ASC -> {
                            sortedCredentials = credentials
                                .sortedWith(
                                    compareBy(
                                        {
                                            if (expiredCredentialsOnTop && it.isExpired(key)) 0 else 1
                                        },
                                        {
                                            SecretService.decryptCommonString(key, it.name)
                                                .lowercase()
                                        }
                                    )
                                )
                        }
                        CredentialSortOrder.CREDENTIAL_NAME_DESC -> {
                            sortedCredentials = credentials
                                .sortedWith(
                                    compareBy(
                                        {
                                            if (expiredCredentialsOnTop && it.isExpired(key)) 1 else 0
                                        },
                                        {
                                            SecretService.decryptCommonString(key, it.name)
                                                .lowercase()
                                        }
                                    )
                                ).reversed()
                        }
                        CredentialSortOrder.RECENTLY_MODIFIED -> {
                            sortedCredentials = credentials
                                .sortedWith(
                                    compareBy(
                                        {
                                            if (expiredCredentialsOnTop && it.isExpired(key)) 1 else 0
                                        },
                                        {
                                            it.modifyTimestamp
                                        }
                                    )
                                ).reversed()
                        }
                        CredentialSortOrder.CREDENTIAL_IDENTIFIER -> {
                            sortedCredentials = credentials
                                .sortedWith(
                                    compareBy(
                                        {
                                            if (expiredCredentialsOnTop && it.isExpired(key)) 0 else 1
                                        },
                                        {
                                            it.id
                                        }
                                    )
                                )
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
            else {
                listCredentialAdapter?.filter?.filter("")
            }
        }
    }

    private fun refreshMenuMasterPasswordItem(menu: Menu) {
        if (!navMenuQuickAccessVisible) {
            return
        }
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
        credential.id?.let { id ->
            credentialViewModel.deleteExpiredCredential(id, this)
        }
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

    private fun updateResumeAutofillMenuItem() {
        resumeAutofillItem?.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ResponseFiller.isAutofillPaused(this)
    }

    private fun updateNavigationMenuVisibility(groupResId: Int, itemResId: Int, stringResId: Int, visible: Boolean) {
        navigationView.menu.setGroupVisible(groupResId, visible)
        navigationView.menu.setGroupEnabled(groupResId, visible)

        val item = navigationView.menu.findItem(itemResId)
        item.isEnabled = true
        item.isVisible = true
        val s =
            SpannableString(getString(stringResId) + (if (visible) " ▲" else " ▼"))
        s.setSpan(
            StyleSpan(Typeface.BOLD), 0, s.length, 0
        )
        s.setSpan(
           ForegroundColorSpan(getColor(android.R.color.darker_gray/*R.color.colorAltAccent*/)), 0, s.length, 0
        )
        item.title = s
    }

    private fun setNavMenuCollapsed() {
        navMenuQuickAccessVisible = false
        navMenuExportVisible = false
        navMenuImportVisible = false
        navMenuVaultVisible = false
    }

    fun searchForExpiredCredentials() {
        startSearchFor(SEARCH_COMMAND_SHOW_EXPIRED + " ") // space at the end to not show suggestion menu popup
    }

    private fun startSearchFor(searchString: String): Boolean {
        Log.i("LST", "searchItem=$searchItem")
        searchItem?.let { searchItem ->
            Log.i("LST", "update search text")

            val searchView =
                MenuItemCompat.getActionView(searchItem) as SearchView
            val searchPlate =
                searchView.findViewById(R.id.search_src_text) as EditText

            searchView.setQuery(searchString, true)
            searchItem.expandActionView()
            searchPlate.text = SpannableStringBuilder(searchString)
            searchPlate.selectAll()
            return true
        }
        return false
    }
}

