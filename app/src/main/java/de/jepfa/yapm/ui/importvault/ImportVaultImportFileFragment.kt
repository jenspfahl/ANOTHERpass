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
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.MasterKeyService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase
import de.jepfa.yapm.util.*

class ImportVaultImportFileFragment : BaseFragment() {

    private lateinit var mkTextView: TextView
    private var encMasterKey: String? = null

    init {
        enableBack = true
        backToPreviousFragment = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_import_vault_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)
        setTitle(R.string.import_vault_import_file_fragment_label)

        val loadedFileStatusTextView = view.findViewById<TextView>(R.id.loaded_file_status)
        val scanQrCodeImageView = view.findViewById<ImageView>(R.id.imageview_scan_qrcode)
        scanQrCodeImageView.setOnClickListener {
            QRCodeUtil.scanQRCode(this, getString(R.string.scanning_emk))
        }

        val scanNfcImageView: ImageView = view.findViewById(R.id.imageview_scan_nfc)
        if (!NfcService.isNfcAvailable(getBaseActivity())) {
            scanNfcImageView.visibility = View.GONE
        }
        scanNfcImageView.setOnClickListener {
            NfcService.scanNfcTag(this)
        }

        mkTextView = view.findViewById(R.id.text_scan_mk)

        val jsonContent = getImportVaultActivity().jsonContent ?: return

        val createdAt = jsonContent.get(VaultExportService.JSON_CREATION_DATE)?.asString
        val credentialsCount = jsonContent.get(VaultExportService.JSON_CREDENTIALS_COUNT)?.asString
        val labelsCount = jsonContent.get(VaultExportService.JSON_LABELS_COUNT)?.asString
        val salt = jsonContent.get(VaultExportService.JSON_VAULT_ID)
        val vaultId = if (salt != null) SaltService.saltToVaultId(salt.asString) else R.string.unknown_placeholder
        encMasterKey = jsonContent.get(VaultExportService.JSON_ENC_MK)?.asString

        var mkProvidedText = ""
        if (encMasterKey != null) {
            mkTextView.text = encMasterKey
            mkProvidedText = getString(R.string.emk_provided)
        }
        else {
            mkProvidedText = getString(R.string.emk_not_provided)
        }
        loadedFileStatusTextView.text = getString(R.string.vault_export_info,
            formatAsDate(createdAt), credentialsCount, labelsCount, vaultId)
            .plus(System.lineSeparator())
            .plus(System.lineSeparator())
            .plus(mkProvidedText)


        val importButton = view.findViewById<Button>(R.id.button_import_loaded_vault)

        importButton.setOnClickListener {
            if (mkTextView.text.isEmpty() || encMasterKey == null) {
                toastText(getBaseActivity(), R.string.scan_masterkey_first)
                return@setOnClickListener
            }

            if (MasterKeyService.isMasterKeyStored(getImportVaultActivity())) {
                toastText(getBaseActivity(), R.string.vault_already_created)
                return@setOnClickListener
            }

            val importVaultActivity: ImportVaultActivity? = getBaseActivityAs()
            importVaultActivity?.let {
                importVault(jsonContent, encMasterKey, it)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val scanned = getScannedFromIntent(requestCode, resultCode, data)

        if (scanned != null) {
            val encrypted = Encrypted.fromEncryptedBase64StringWithCheck(scanned)
            if (encrypted != null && encrypted.isType(ENC_MASTER_KEY)) {
                encMasterKey = scanned
                mkTextView.text = encMasterKey
            }
            else {
                toastText(getBaseActivity(), R.string.not_an_emk)
                return
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getImportVaultActivity() : ImportVaultActivity {
        return getBaseActivity() as ImportVaultActivity
    }

    private fun importVault(
        jsonContent: JsonObject,
        encMasterKey: String?,
        activity: SecureActivity
    ) {
        UseCaseBackgroundLauncher(ImportVaultUseCase)
            .launch(activity, ImportVaultUseCase.Input(
                jsonContent,
                encMasterKey,
                override = false)
            )
            { output ->
                if (!output.success) {
                    toastText(context, R.string.something_went_wrong)
                }
                else {
                    findNavController().navigate(R.id.action_importVault_to_Login)
                    getBaseActivity()?.finish()
                }
            }
    }

    private fun getScannedFromIntent(requestCode: Int, resultCode: Int, data: Intent?): String? {
        if (requestCode == NfcActivity.ACTION_READ_NFC_TAG) {
            return data?.getStringExtra(NfcActivity.EXTRA_SCANNED_NDC_TAG_DATA)
        }
        else {
            return QRCodeUtil.extractContentFromIntent(requestCode, resultCode, data)
        }
        return null
    }
}