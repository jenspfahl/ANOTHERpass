package de.jepfa.yapm.ui.importcredentials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.label.LabelEditViewExtender
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.usecase.credential.ImportCredentialsUseCase
import de.jepfa.yapm.util.toastText

class ImportCredentialsImportFileFragment : BaseFragment() {

    init {
        enableBack = true
        backToPreviousFragment = true
    }

    private lateinit var labelEditViewExtender: LabelEditViewExtender
    private lateinit var expandableListView: ExpandableListView
    private lateinit var adapter: ImportCredentialsImportFileAdapter
    private var credentialsToBeImported: MutableSet<ImportCredentialsImportFileAdapter.FileRecord> = HashSet()


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_import_credentials, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)
        setTitle(R.string.import_credentials_import_file_fragment_label)

        val importCredentialsActivity = getImportCredentialsActivity()
        val loadedFileStatusTextView = view.findViewById<TextView>(R.id.loaded_file_status)
        labelEditViewExtender = LabelEditViewExtender(importCredentialsActivity, view)
        savedInstanceState?.getStringArray("added_labels")?.let { labelNames ->
            val labels = labelNames.mapNotNull { LabelService.defaultHolder.lookupByLabelName(it) }.toList()
            labelEditViewExtender.addPersistedLabels(labels)
        }

    //    val jsonContent = getImportCredentialsActivity().parsedVault?.content ?: return

  /*      val createdAt = jsonContent.get(VaultExportService.JSON_CREATION_DATE)?.asString
        val credentialsCount = jsonContent.get(VaultExportService.JSON_CREDENTIALS_COUNT)?.asString ?: 0
        val labelsCount = jsonContent.get(VaultExportService.JSON_LABELS_COUNT)?.asString ?: 0
*/
        loadedFileStatusTextView.text = "2 credentials mockup"//getString(R.string.vault_export_info2,
         //   formatAsDate(createdAt, importCredentialsActivity), credentialsCount, labelsCount)

        credentialsToBeImported.clear()
        credentialsToBeImported.add(ImportCredentialsImportFileAdapter.FileRecord(1, "test1", "http://test1.de", "jens1", "xcvsdf1"))
        credentialsToBeImported.add(ImportCredentialsImportFileAdapter.FileRecord(2, "test2", "http://test2.de", "jens2", "3egsdg"))

        createExpandableView(
            credentialsToBeImported.toList().sortedBy { it.name },
            view,
            importCredentialsActivity,
            savedInstanceState
        )


        val importButton = view.findViewById<Button>(R.id.button_import_loaded_credentials)

        importButton.setOnClickListener {

            val credentialsToImport = adapter.checkedChildren

            if (credentialsToImport.isEmpty()) {
                toastText(importCredentialsActivity, R.string.nothing_to_import)
                return@setOnClickListener
            }

            AlertDialog.Builder(importCredentialsActivity)
                .setTitle(R.string.import_credentials_file)
                .setMessage(importCredentialsActivity.getString(R.string.message_import_credential_records, credentialsToImport.size))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialog, whichButton ->

                    importCredentialsActivity.masterSecretKey?.let { key ->
                        importExternalCredentials(
                            credentialsToImport
                                .map {
                                    importCredentialsActivity.createCredentialFromRecord(
                                        key,
                                        it,
                                        labelEditViewExtender.getCommitedLabelNames()
                                    )
                                }
                                .toList(), importCredentialsActivity)
                    }

                }
                .setNegativeButton(android.R.string.no, null)
                .show()



        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("records_list", adapter.checkedChildren.map { it.id }.toIntArray())
        outState.putBoolean("records_list_view", expandableListView.isGroupExpanded(0))
        outState.putStringArray("added_labels", labelEditViewExtender.getCommitedLabelNames().toTypedArray())

    }

    private fun createExpandableView(
        children: List<ImportCredentialsImportFileAdapter.FileRecord>,
        view: View,
        importCredentialsActivity: ImportCredentialsActivity,
        savedInstanceState: Bundle?
    ): Set<ImportCredentialsImportFileAdapter.FileRecord> {

        expandableListView =
            view.findViewById(R.id.expandable_list_credentials)

        val selectNoneAll =
            view.findViewById<CheckBox>(R.id.button_select_credentials)

        val checkedChildren = fillChildrenFromState(children, savedInstanceState)
        adapter = ImportCredentialsImportFileAdapter(
                selectNoneAll,
                importCredentialsActivity,
                children,
                checkedChildren
            )
        expandableListView.setAdapter(adapter)
        if (children.size < 5) {
            expandableListView.expandGroup(0)
        }

        savedInstanceState?.getBoolean("records_list_view")?.let { expanded ->
            if (expanded) {
                expandableListView.expandGroup(0)
            }
            else {
                expandableListView.collapseGroup(0)
            }
        }

        selectNoneAll.setOnClickListener {
            adapter.selectNoneAllClicked()
        }

        return adapter.checkedChildren
    }

    private fun fillChildrenFromState(
        children: List<ImportCredentialsImportFileAdapter.FileRecord>,
        savedInstanceState: Bundle?
    ): MutableSet<ImportCredentialsImportFileAdapter.FileRecord> {
       val ids = savedInstanceState?.getIntArray("records_list")?.toList()
            ?: return children.toMutableSet()

        return ids.mapNotNull { id ->
            children.find { it.id == id }
        }.toMutableSet()
    }

    private fun getImportCredentialsActivity() : ImportCredentialsActivity {
        return getBaseActivity() as ImportCredentialsActivity
    }

    private fun importExternalCredentials(
        credentials: List<EncCredential>,
        activity: SecureActivity
    ) {
       UseCaseBackgroundLauncher(ImportCredentialsUseCase)
            .launch(activity, ImportCredentialsUseCase.Input(credentials)
            ) { output ->
                if (!output.success) {
                    toastText(context, R.string.something_went_wrong)
                } else {
                    getBaseActivity()?.finish()
                }
            }
    }

}