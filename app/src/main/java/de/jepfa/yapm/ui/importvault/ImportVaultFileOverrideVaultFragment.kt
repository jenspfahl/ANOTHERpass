package de.jepfa.yapm.ui.importvault

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.google.gson.JsonObject
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.importvault.ImportVaultFileOverrideVaultNamedAdapter.GroupType
import de.jepfa.yapm.ui.importvault.ImportVaultFileOverrideVaultNamedAdapter.ChildType
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase
import de.jepfa.yapm.util.toastText

class ImportVaultFileOverrideVaultFragment : BaseFragment() {

    init {
        enableBack = true
        backToPreviousFragment = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_import_vault_file_override_vault, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)
        setTitle(R.string.import_vault_import_file_fragment_label)

        val importVaultActivity = getImportVaultActivity()
        val loadedFileStatusTextView = view.findViewById<TextView>(R.id.loaded_file_status)

        val jsonContent = getImportVaultActivity().jsonContent
        if (jsonContent == null) {
            return
        }

        val createdAt = jsonContent.get(VaultExportService.JSON_CREATION_DATE)?.asString
        val credentialsCount = jsonContent.get(VaultExportService.JSON_CREDENTIALS_COUNT)?.asString
        val labelsCount = jsonContent.get(VaultExportService.JSON_LABELS_COUNT)?.asString
        val salt = jsonContent.get(VaultExportService.JSON_VAULT_ID)
        val vaultId = if (salt != null) SaltService.saltToVaultId(salt.asString) else R.string.unknown_placeholder

        loadedFileStatusTextView.text = getString(R.string.vault_export_info, createdAt, credentialsCount, labelsCount, vaultId)

        var credentialsToOverride = emptySet<ChildType>()
        var labelsToOverride = emptySet<ChildType>()

        if (credentialsCount?.toIntOrNull() ?:0 > 0) {
            importVaultActivity.credentialViewModel.allCredentials.observe(importVaultActivity) { existingCredentials ->

                val existingCredentialIds =
                    existingCredentials.filter { it.id != null }.map { it.id!! }
                val credentialsJson =
                    jsonContent.getAsJsonArray(VaultExportService.JSON_CREDENTIALS)

                val credentialsToBeInserted = credentialsJson
                    .map { json -> EncCredential.fromJson(json) }
                    .filterNotNull()
                    .filterNot { existingCredentialIds.contains(it.id) }
                    .map { ChildType(it.id!!, null, it) }

                val credentialsToBeUpdated = credentialsJson
                    .map { json -> EncCredential.fromJson(json) }
                    .filterNotNull()
                    .filter {
                        existingCredentialIds.contains(it.id)
                                && !existingCredentials.contains(it) // Note:this compares the whole data of a credential
                    }
                    .map { c -> ChildType(c.id!!, existingCredentials.find { it.id == c.id }, c) }

                val credentialChanges = HashMap<GroupType, List<ChildType>>()
                credentialChanges[GroupType.CREDENTIALS_TO_BE_INSERTED] = credentialsToBeInserted
                credentialChanges[GroupType.CREDENTIALS_TO_BE_UPDATED] = credentialsToBeUpdated
                val expandableListViewNewCreds =
                    view.findViewById<ExpandableListView>(R.id.expandable_list_credentials)
                val credentialAdapter =
                    ImportVaultFileOverrideVaultNamedAdapter(
                        importVaultActivity,
                        credentialChanges
                    )
                expandableListViewNewCreds.setAdapter(credentialAdapter)
                credentialsToOverride = credentialAdapter.getCheckedChildren()
            }
        }

        if (labelsCount?.toIntOrNull() ?:0 > 0) {
            importVaultActivity.labelViewModel.allLabels.observe(importVaultActivity) { existingLabels ->

                val existingLabelIds =
                    existingLabels.filter { it.id != null }.map { it.id!! }
                val labelsJson =
                    jsonContent.getAsJsonArray(VaultExportService.JSON_LABELS)

                val labelsToBeInserted = labelsJson
                    .map { json -> EncLabel.fromJson(json) }
                    .filterNotNull()
                    .filterNot { existingLabelIds.contains(it.id) }
                    .map { ChildType(it.id!!, null, it) }

                val labelsToBeUpdated = labelsJson
                    .map { json -> EncLabel.fromJson(json) }
                    .filterNotNull()
                    .filter {
                        existingLabelIds.contains(it.id)
                                && !existingLabels.contains(it) // Note:this compares the whole data of a label
                    }
                    .map { l -> ChildType(l.id!!, existingLabels.find { it.id == l.id }, l) }

                val labelChanges = HashMap<GroupType, List<ChildType>>()
                labelChanges[GroupType.LABELS_TO_BE_INSERTED] = labelsToBeInserted
                labelChanges[GroupType.LABELS_TO_BE_UPDATED] = labelsToBeUpdated
                val expandableListViewNewCreds =
                    view.findViewById<ExpandableListView>(R.id.expandable_list_labels)
                val labelAdapter =
                    ImportVaultFileOverrideVaultNamedAdapter(
                        importVaultActivity,
                        labelChanges
                    )
                expandableListViewNewCreds.setAdapter(labelAdapter)
                labelsToOverride = labelAdapter.getCheckedChildren()
            }
        }

        val copyOrigSwitch = view.findViewById<SwitchCompat>(R.id.switch_copy_orig)
        val importButton = view.findViewById<Button>(R.id.button_import_loaded_vault)

        importButton.setOnClickListener {

            val importVaultActivity: ImportVaultActivity? = getBaseActivityAs()

            if (credentialsToOverride.isEmpty() && labelsToOverride.isEmpty()) {
                toastText(importVaultActivity, R.string.nothing_to_import)
                return@setOnClickListener
            }

            importVaultActivity?.let{ activity ->
                AlertDialog.Builder(activity)
                    .setTitle(R.string.import_vault_as_file)
                    .setMessage(activity.getString(R.string.message_import_vault_records, credentialsToOverride.size, labelsToOverride.size))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { dialog, whichButton ->

                        importVaultActivity?.let { activity ->
                            PreferenceService
                                .getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, activity)
                                ?.let { encMasterKey ->
                                    importVault(
                                        jsonContent,
                                        encMasterKey,
                                        credentialsToOverride.map { it.id }.toSet(),
                                        labelsToOverride.map { it.id }.toSet(),
                                        copyOrigSwitch.isChecked,
                                        activity)
                                }
                        }

                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()

            }

        }
    }

    private fun getImportVaultActivity() : ImportVaultActivity {
        return getBaseActivity() as ImportVaultActivity
    }

    private fun importVault(
        jsonContent: JsonObject,
        encMasterKey: Encrypted,
        credentialIdsToOverride: Set<Int>,
        labelIdsToOverride: Set<Int>,
        copyOrigin: Boolean,
        activity: SecureActivity
    ) {
        UseCaseBackgroundLauncher(ImportVaultUseCase)
            .launch(activity, ImportVaultUseCase.Input(
                jsonContent,
                encMasterKey.toBase64String(),
                credentialIdsToOverride = credentialIdsToOverride,
                labelIdsToOverride = labelIdsToOverride,
                override = true,
                copyOrigin = copyOrigin)
            )
            { output ->
                if (!output.success) {
                    toastText(context, R.string.something_went_wrong)
                }
                else {
                    getBaseActivity()?.finish()
                }
            }
    }

}