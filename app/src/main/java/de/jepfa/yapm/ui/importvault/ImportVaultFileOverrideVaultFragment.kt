package de.jepfa.yapm.ui.importvault

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.google.gson.JsonObject
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.NonScrollExpandableListView
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

    private val adapters = HashMap<GroupType, ImportVaultFileOverrideVaultNamedAdapter>()
    private val expandableViews = HashMap<GroupType, ExpandableListView>()

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

        val jsonContent = getImportVaultActivity().jsonContent ?: return

        val createdAt = jsonContent.get(VaultExportService.JSON_CREATION_DATE)?.asString
        val credentialsCount = jsonContent.get(VaultExportService.JSON_CREDENTIALS_COUNT)?.asString
        val labelsCount = jsonContent.get(VaultExportService.JSON_LABELS_COUNT)?.asString
        val salt = jsonContent.get(VaultExportService.JSON_VAULT_ID)
        val vaultId = if (salt != null) SaltService.saltToVaultId(salt.asString) else R.string.unknown_placeholder

        loadedFileStatusTextView.text = getString(R.string.vault_export_info, createdAt, credentialsCount, labelsCount, vaultId)

        var credentialsToInsert: Set<ChildType>? = null
        var credentialsToUpdate: Set<ChildType>? = null
        var labelsToInsert: Set<ChildType>? = null
        var labelsToUpdate: Set<ChildType>? = null

        LabelService.externalHolder.clearAll()

        if (credentialsCount?.toIntOrNull() ?:0 > 0) {
            importVaultActivity.credentialViewModel.allCredentials.observe(importVaultActivity) { existingCredentials ->

                val existingCredentialIds =
                    existingCredentials.filter { it.id != null }.map { it.id!! }
                val credentialsJson =
                    jsonContent.getAsJsonArray(VaultExportService.JSON_CREDENTIALS)

                val externalCredentials = credentialsJson
                    .map { json -> EncCredential.fromJson(json) }
                    .filterNotNull()

                val credentialsToBeInserted = externalCredentials
                    .filterNot { existingCredentialIds.contains(it.id) }
                    .map { ChildType(it.id!!, null, it) }

                val credentialsToBeUpdated =externalCredentials
                    .filter {
                        existingCredentialIds.contains(it.id)
                                && !existingCredentials.contains(it) // Note:this compares the whole data of a credential
                    }
                    .filterNot { isContentEqualTo(importVaultActivity.masterSecretKey, it, existingCredentials) }
                    .map { c -> ChildType(c.id!!, existingCredentials.find { it.id == c.id }, c) }

                credentialsToInsert = createExpandableView(
                    credentialsToBeInserted,
                    GroupType.CREDENTIALS_TO_BE_INSERTED,
                    view,
                    R.id.expandable_list_insert_credentials,
                    R.id.button_select_none_all_insert_credentials,
                    importVaultActivity,
                    savedInstanceState
                )

                credentialsToUpdate = createExpandableView(
                    credentialsToBeUpdated,
                    GroupType.CREDENTIALS_TO_BE_UPDATED,
                    view,
                    R.id.expandable_list_update_credentials,
                    R.id.button_select_none_all_update_credentials,
                    importVaultActivity,
                    savedInstanceState
                )
            }
        }

        if (labelsCount?.toIntOrNull() ?:0 > 0) {
            importVaultActivity.labelViewModel.allLabels.observe(importVaultActivity) { existingLabels ->

                val existingLabelIds =
                    existingLabels.filter { it.id != null }.map { it.id!! }
                val labelsJson =
                    jsonContent.getAsJsonArray(VaultExportService.JSON_LABELS)

                val externalLabels = labelsJson
                    .map { json -> EncLabel.fromJson(json) }
                    .filterNotNull()

                importVaultActivity.masterSecretKey?.let {
                    LabelService.externalHolder.initLabels(it, externalLabels.toSet())
                }

                val labelsToBeInserted = externalLabels
                    .filterNot { existingLabelIds.contains(it.id) }
                    .map { ChildType(it.id!!, null, it) }

                val labelsToBeUpdated = externalLabels
                    .filter {
                        existingLabelIds.contains(it.id)
                                && !existingLabels.contains(it) // Note:this compares the whole data of a label
                    }
                    .filterNot { isContentEqualTo(importVaultActivity.masterSecretKey, it) }
                    .map { l -> ChildType(l.id!!, existingLabels.find { it.id == l.id }, l) }

                labelsToInsert = createExpandableView(
                    labelsToBeInserted,
                    GroupType.LABELS_TO_BE_INSERTED,
                    view,
                    R.id.expandable_list_insert_labels,
                    R.id.button_select_none_all_insert_labels,
                    importVaultActivity,
                    savedInstanceState
                )

                labelsToUpdate = createExpandableView(
                    labelsToBeUpdated,
                    GroupType.LABELS_TO_BE_UPDATED,
                    view,
                    R.id.expandable_list_update_labels,
                    R.id.button_select_none_all_update_labels,
                    importVaultActivity,
                    savedInstanceState
                )
            }
        }

        val copyOrigSwitch = view.findViewById<SwitchCompat>(R.id.switch_copy_orig)
        val importButton = view.findViewById<Button>(R.id.button_import_loaded_vault)

        importButton.setOnClickListener {

            val importVaultActivity: ImportVaultActivity? = getBaseActivityAs()

            val credentialsToOverride = HashSet<ChildType>()
            credentialsToInsert?.let {
                credentialsToOverride.addAll(it)
            }
            credentialsToUpdate?.let {
                credentialsToOverride.addAll(it)
            }

            val labelsToOverride = HashSet<ChildType>()
            labelsToInsert?.let {
                labelsToOverride.addAll(it)
            }
            labelsToUpdate?.let {
                labelsToOverride.addAll(it)
            }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        adapters.forEach { (k, v) ->
            outState.putIntArray(k.toString(), v.checkedChildren.map { it.id }.toIntArray())
        }
        expandableViews.forEach { (k, v) ->
            outState.putBoolean(k.toString() + "_view", v.isGroupExpanded(0))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LabelService.externalHolder.clearAll()
    }

    private fun isContentEqualTo(
        masterSecretKey: SecretKeyHolder?,
        externalCredential: EncCredential,
        existingCredentials: List<EncCredential>): Boolean {
        if (masterSecretKey == null) {
            return false
        }
        
        val externalName = SecretService.decryptCommonString(masterSecretKey, externalCredential.name)
        val externalPasswd = SecretService.decryptPassword(masterSecretKey, externalCredential.password)
        val externalUser = SecretService.decryptCommonString(masterSecretKey, externalCredential.user)
        val externalWebsite = SecretService.decryptCommonString(masterSecretKey, externalCredential.website)
        val externalAdditionalInfo =
            SecretService.decryptCommonString(masterSecretKey, externalCredential.additionalInfo)
        val externalLabelIds =
            LabelService.externalHolder.decryptLabelsIdsForCredential(masterSecretKey, externalCredential)

        val existingCredential = existingCredentials.find { it.id == externalCredential.id }
            ?: return false

        val existingName = SecretService.decryptCommonString(masterSecretKey, existingCredential.name)
        val existingPasswd = SecretService.decryptPassword(masterSecretKey, existingCredential.password)
        val existingUser = SecretService.decryptCommonString(masterSecretKey, existingCredential.user)
        val existingWebsite = SecretService.decryptCommonString(masterSecretKey, existingCredential.website)
        val existingAdditionalInfo =
            SecretService.decryptCommonString(masterSecretKey, existingCredential.additionalInfo)
        val existingLabelIds =
            LabelService.defaultHolder.decryptLabelsIdsForCredential(masterSecretKey, existingCredential)

        val contentEquals = externalName == existingName
                && externalPasswd.isEqual(existingPasswd)
                && externalUser == existingUser
                && externalWebsite == existingWebsite
                && externalAdditionalInfo == existingAdditionalInfo
                && externalLabelIds == existingLabelIds
                && externalCredential.isObfuscated == existingCredential.isObfuscated

        externalPasswd.clear()
        existingPasswd.clear()

        return contentEquals
    }

    private fun isContentEqualTo(
        masterSecretKey: SecretKeyHolder?,
        externalLabel: EncLabel): Boolean {
        if (masterSecretKey == null) {
            return false
        }

        val externalName = SecretService.decryptCommonString(masterSecretKey, externalLabel.name)
        val externalDescription = SecretService.decryptCommonString(masterSecretKey, externalLabel.description)


        val existingLabel = LabelService.defaultHolder.lookupByLabelId(externalLabel.id!!) ?: return false

        val existingName = existingLabel.name
        val existingDescription = existingLabel.description

        return externalName == existingName
                && externalDescription == existingDescription
                && externalLabel.color == existingLabel.colorRGB
    }


    private fun createExpandableView(
        children: List<ChildType>,
        groupType: GroupType,
        view: View,
        expandableListViewId: Int,
        selectNoneAllCheckBoxId: Int,
        importVaultActivity: ImportVaultActivity,
        savedInstanceState: Bundle?
    ): Set<ChildType> {

        val expandableListView =
            view.findViewById<ExpandableListView>(expandableListViewId)
        expandableViews[groupType] = expandableListView

        val selectNoneAll =
            view.findViewById<CheckBox>(selectNoneAllCheckBoxId)

        val checkedChildren = fillChildrenFromState(groupType, children, savedInstanceState)
        val adapter = ImportVaultFileOverrideVaultNamedAdapter(
                selectNoneAll,
                importVaultActivity,
                groupType,
                children,
                checkedChildren
            )
        expandableListView.setAdapter(adapter)
        adapters[groupType] = adapter

        savedInstanceState?.getBoolean(groupType.toString() + "_view")?.let { expanded ->
            if (expanded) {
                expandableListView.expandGroup(0)
            }
        }

        selectNoneAll.setOnClickListener {
            adapter.selectNoneAllClicked()
        }

        return adapter.checkedChildren
    }

    private fun fillChildrenFromState(
        groupType: GroupType,
        children: List<ChildType>,
        savedInstanceState: Bundle?
    ): MutableSet<ChildType> {
        val ids = savedInstanceState?.getIntArray(groupType.toString())?.toList()
            ?: return children.toMutableSet()

        return ids.mapNotNull { id ->
            children.find { it.id == id }
        }.toMutableSet()
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