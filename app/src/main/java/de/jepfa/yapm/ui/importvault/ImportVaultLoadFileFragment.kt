package de.jepfa.yapm.ui.importvault

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.usecase.vault.ImportVaultUseCase
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.toastText

class ImportVaultLoadFileFragment : BaseFragment() {

    private val importVaultFile = 1

    init {
        enableBack = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_load_vault_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)
        setTitle(R.string.import_vault_load_file_fragment_label)

        val importVaultActivity = getImportVaultActivity()

        val explanation = view.findViewById<TextView>(R.id.import_vault_explanation)
        if (importVaultActivity.isOverrideMode()) {
            explanation.text = getString(R.string.import_vault_to_vault_explanation, SaltService.getVaultId(importVaultActivity))
        }

        val loadButton = view.findViewById<Button>(R.id.button_load_vault)
        loadButton.setOnClickListener {
            PermissionChecker.verifyReadStoragePermissions(importVaultActivity)
            if (PermissionChecker.hasReadStoragePermissions(importVaultActivity)) {
                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_select_file_to_import))
                startActivityForResult(chooserIntent, importVaultFile)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val importVaultActivity = getImportVaultActivity()

        if (resultCode == RESULT_OK && requestCode == importVaultFile) {
            data?.let {
                val selectedFile = data.data

                if (selectedFile != null && FileUtil.isExternalStorageReadable()) {
                    try {
                        val baseActivity = getBaseActivity() ?: return
                        val content = FileUtil.readFile(baseActivity, selectedFile)
                        if (content != null) {
                            getImportVaultActivity().parsedVault = ImportVaultUseCase.parseVaultFileContent(
                                content, importVaultActivity, handleBlob = true)
                        }
                    } catch (e: Exception) {
                        Log.e("RESTORE", "cannot import file $selectedFile", e)
                    }
                }
            }

            val parsedVault = importVaultActivity.parsedVault
            if (parsedVault == null) {
                toastText(activity, R.string.toast_import_vault_failure)
            }
            else if ((parsedVault.appVersionCode ?: 0) > DebugInfo.getVersionCode(importVaultActivity)) {
                toastText(activity, R.string.toast_import_vault_from_future_app_version)
            }
            else if (importVaultActivity.isOverrideMode() && parsedVault.vaultId != null && !sameVaultId(parsedVault.vaultId, importVaultActivity)) {
                toastText(activity, R.string.toast_import_vault_failure_no_vault_match)
            }
            else if(parsedVault.cipherAlgorithm != null && !cipherVersionSupported(parsedVault.cipherAlgorithm)) {
                toastText(activity, R.string.toast_import_vault_failure_cipher_not_supported)
            }
            else if (importVaultActivity.isOverrideMode() && parsedVault.cipherAlgorithm != null && !sameCipherAlgorithm(parsedVault.cipherAlgorithm, importVaultActivity)) {
                toastText(activity, R.string.toast_import_vault_failure_not_same_algo)
            }
            else if (parsedVault.content == null || parsedVault.vaultId == null || parsedVault.cipherAlgorithm == null) {
                toastText(activity, R.string.toast_import_vault_failure)
            }
            else {
                if (importVaultActivity.isOverrideMode()) {
                    findNavController().navigate(R.id.action_importVault_LoadFileFragment_to_ImportFileOVerrideVaultFragment)
                }
                else {
                    findNavController().navigate(R.id.action_importVault_LoadFileFragment_to_ImportFileFragment)
                }
            }
        }
    }

    private fun sameVaultId(vaultId: String, context: Context): Boolean {
        val currentSaltAsVaultId = SaltService.getSaltAsBase64String(context)
        return vaultId == currentSaltAsVaultId
    }

    private fun sameCipherAlgorithm(cipherAlgorithm: CipherAlgorithm, context: Context): Boolean {
        val currentAlgo = SecretService.getCipherAlgorithm(context)
        return cipherAlgorithm == currentAlgo
    }

    private fun cipherVersionSupported(cipherAlgorithm: CipherAlgorithm): Boolean {
        return Build.VERSION.SDK_INT >= cipherAlgorithm.supportedSdkVersion
    }

    private fun getImportVaultActivity() : ImportVaultActivity {
        return getBaseActivity() as ImportVaultActivity
    }

}