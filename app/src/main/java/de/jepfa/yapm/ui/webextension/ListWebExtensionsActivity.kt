package de.jepfa.yapm.ui.webextension

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.usecase.webextension.DeleteWebExtensionUseCase

class ListWebExtensionsActivity : SecureActivity() {

    private lateinit var listWebExtensionsAdapter: ListWebExtensionsAdapter

    init {
        enableBack = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_web_extensions)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        listWebExtensionsAdapter = ListWebExtensionsAdapter(this)
        recyclerView.adapter = listWebExtensionsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        val showDividers = PreferenceService.getAsBool(PreferenceService.PREF_SHOW_DIVIDERS_IN_LIST, this)
        if (showDividers) {
            val dividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                DividerItemDecoration.VERTICAL
            )
            recyclerView.addItemDecoration(dividerItemDecoration)
        }

        val prefSortOrder = getPrefSortOrder()
        val enabledOnTop = PreferenceService.getAsBool(PreferenceService.PREF_ENABLED_WEB_EXTENSIONS_ON_TOP, this)


        webExtensionViewModel.allWebExtensions.observe(this) { webExtensions ->
            masterSecretKey?.let { key ->
                var sorted = webExtensions
                    .filter { it.linked }

                sorted = when (prefSortOrder) {
                    WebExtensionSortOrder.TITLE -> sorted.sortedWith(
                        compareBy (
                            { if (enabledOnTop && it.enabled) 0 else 1 },
                            { SecretService.decryptCommonString(key, it.title).lowercase() }
                        )
                    )
                    WebExtensionSortOrder.WEB_CLIENT_ID -> sorted.sortedWith(
                        compareBy (
                            { if (enabledOnTop && it.enabled) 0 else 1 },
                            { SecretService.decryptCommonString(key, it.webClientId).lowercase() }
                        )
                    )
                    WebExtensionSortOrder.RECENTLY_USED -> sorted.sortedWith(
                        compareBy (
                            { if (enabledOnTop && it.enabled) 1 else 0 },
                            { it.lastUsedTimestamp }
                        )
                    ).reversed()
                }


                // delete all unlinked where the linking was aborted
                webExtensions
                    .filter { !it.linked }
                    .forEach {
                        UseCaseBackgroundLauncher(DeleteWebExtensionUseCase)
                            .launch(this, it)
                    }

                listWebExtensionsAdapter.submitList(sorted)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (checkSession && Session.isDenied()) {
            return false
        }

        menuInflater.inflate(R.menu.menu_web_extension_list, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (checkSession && Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return false
        }

        if (id == R.id.menu_info_about_web_extensions) {
            WebExtensionDialogs.openInfoDialog(this)
            return true
        }

        if (id == R.id.menu_delete_disabled_web_extensions) {
            WebExtensionDialogs.openDeleteDisabledWebExtension(this)
            return true
        }

        if (id == R.id.menu_sort_web_extensions) {
            val prefSortOrder = getPrefSortOrder()
            val listItems = WebExtensionSortOrder.values().map { getString(it.labelId) }.toTypedArray()


            val view = LinearLayout(this)
            view.orientation = LinearLayout.HORIZONTAL
            view.setPadding(54, 32, 64, 16)

            val checkBox = CheckBox(this)
            checkBox.isChecked = PreferenceService.getAsBool(PreferenceService.PREF_ENABLED_WEB_EXTENSIONS_ON_TOP, this)
            checkBox.text = getString(R.string.enabled_links_always_on_top)
            view.addView(checkBox)

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

                    val newSortOrder = WebExtensionSortOrder.values()[selectedSortOrder]
                    PreferenceService.putString(PreferenceService.PREF_WEB_EXTENSION_SORT_ORDER, newSortOrder.name, this)
                    PreferenceService.putBoolean(PreferenceService.PREF_ENABLED_WEB_EXTENSIONS_ON_TOP, checkBox.isChecked, this)

                    recreate()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .show()

            return true
        }




        return super.onOptionsItemSelected(item)
    }

    override fun lock() {
       listWebExtensionsAdapter.submitList(emptyList())
    }


    private fun getPrefSortOrder(): WebExtensionSortOrder {
        val sortOrderAsString = PreferenceService.getAsString(PreferenceService.PREF_WEB_EXTENSION_SORT_ORDER, this)
        if (sortOrderAsString != null) {
            try {
                return WebExtensionSortOrder.valueOf(sortOrderAsString)
            } catch (e: Exception) {
                PreferenceService.delete(PreferenceService.PREF_WEB_EXTENSION_SORT_ORDER, this)
            }
        }
        return WebExtensionSortOrder.DEFAULT
    }
}