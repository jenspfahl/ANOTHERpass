package de.jepfa.yapm.ui.importvault

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType.Types.ENC_MASTER_KEY
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase
import de.jepfa.yapm.util.QRCodeUtil
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


        val importButton = view.findViewById<Button>(R.id.button_import_loaded_vault)

        importButton.setOnClickListener {

            getBaseActivity()?.let { baseActivity ->
                PreferenceService.getEncrypted(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, baseActivity)?.let {
                    importVault(jsonContent, it, baseActivity)
                }
            }
        }
    }

    private fun getImportVaultActivity() : ImportVaultActivity {
        return getBaseActivity() as ImportVaultActivity
    }

    private fun importVault(
        jsonContent: JsonObject,
        encMasterKey: Encrypted,
        activity: BaseActivity
    ) {
        UseCaseBackgroundLauncher(ImportVaultUseCase)
            .launch(activity, ImportVaultUseCase.Input(jsonContent, encMasterKey.toBase64String(), override = true))
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