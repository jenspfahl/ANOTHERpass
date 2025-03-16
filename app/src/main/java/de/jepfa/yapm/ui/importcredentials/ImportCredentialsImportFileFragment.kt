package de.jepfa.yapm.ui.importcredentials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.service.io.CredentialFileRecord
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.label.LabelEditViewExtender
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

        val credentialsToBeImported = importCredentialsActivity.records

        loadedFileStatusTextView.text = getString(R.string.credentials_in_file, credentialsToBeImported?.size?:0)

        createExpandableView(
            credentialsToBeImported?.sortedBy { it.name }?: emptyList(),
            view,
            importCredentialsActivity,
            savedInstanceState
        )


        val importButton = view.findViewById<Button>(R.id.button_import_loaded_credentials)

        importButton.setOnClickListener {

            labelEditViewExtender.commitStaleInput()

            val credentialsToImport = adapter.checkedChildren

            if (credentialsToImport.isEmpty()) {
                toastText(importCredentialsActivity, R.string.nothing_to_import)
                return@setOnClickListener
            }

            AlertDialog.Builder(importCredentialsActivity)
                .setTitle(R.string.import_credentials_file)
                .setMessage(importCredentialsActivity.getString(R.string.message_import_credential_records, credentialsToImport.size))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { _, _ ->

                    importCredentialsActivity.masterSecretKey?.let { key ->

                        val newEncLabels = ArrayList<EncLabel>()
                        val encCredentials = credentialsToImport
                            .map { record ->
                                newEncLabels.addAll(
                                    record.labels
                                      .filter { LabelService.defaultHolder.lookupByLabelName(it) == null } // new label
                                      .map {
                                          var newLabel = LabelService.externalHolder.lookupByLabelName(it)
                                          if (newLabel == null) {
                                              newLabel =
                                                  LabelService.externalHolder.createNewLabel(
                                                      it,
                                                      importCredentialsActivity
                                                  )
                                          }
                                          LabelService.getEncLabelFromLabel(key, newLabel)
                                      }
                                    )

                                importCredentialsActivity.createCredentialFromRecord(
                                    key,
                                    record,
                                    labelEditViewExtender.getCommittedLabelNames(),
                                    LabelService.defaultHolder
                                )
                            }
                            .toList()
                        importExternalCredentials(
                            encCredentials,
                            newEncLabels,
                            importCredentialsActivity)
                    }

                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()



        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("records_list", adapter.checkedChildren.map { it.id }.toIntArray())
        outState.putBoolean("records_list_view", expandableListView.isGroupExpanded(0))
        outState.putStringArray("added_labels", labelEditViewExtender.getCommittedLabelNames().toTypedArray())

    }

    private fun createExpandableView(
        children: List<CredentialFileRecord>,
        view: View,
        importCredentialsActivity: ImportCredentialsActivity,
        savedInstanceState: Bundle?
    ): Set<CredentialFileRecord> {

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
        children: List<CredentialFileRecord>,
        savedInstanceState: Bundle?
    ): MutableSet<CredentialFileRecord> {
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
        labels: List<EncLabel>,
        activity: SecureActivity
    ) {
       UseCaseBackgroundLauncher(ImportCredentialsUseCase)
            .launch(activity, ImportCredentialsUseCase.Input(credentials, labels)
            ) { output ->
                if (!output.success) {
                    toastText(context, R.string.something_went_wrong)
                } else {
                    getBaseActivity()?.finish()
                }
            }
    }

}