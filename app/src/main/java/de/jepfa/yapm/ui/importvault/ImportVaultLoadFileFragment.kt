package de.jepfa.yapm.ui.importvault

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.jepfa.yapm.R
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.usecase.ImportVaultUseCase
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.FileUtil

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

        val loadButton = view.findViewById<Button>(R.id.button_load_vault)
        loadButton.setOnClickListener {
            val baseActivity = getBaseActivity() ?: return@setOnClickListener
            PermissionChecker.verifyReadStoragePermissions(baseActivity)
            if (PermissionChecker.hasReadStoragePermissions(baseActivity)) {
                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_select_vault_file))
                startActivityForResult(chooserIntent, importVaultFile)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == importVaultFile) {
            data?.let {
                val selectedFile = data.data

                if (selectedFile != null && FileUtil.isExternalStorageReadable()) {
                    try {
                        val baseActivity = getBaseActivity() ?: return
                        val content = FileUtil.readFile(baseActivity, selectedFile)
                        if (content != null) {
                            getImportVaultActivity().jsonContent = JsonParser.parseString(content).asJsonObject
                        }
                    } catch (e: Exception) {
                        Log.e("RESTORE", "cannot import file $selectedFile", e)
                    }
                }
            }

            if (getImportVaultActivity().jsonContent == null) {
                Toast.makeText(activity, getString(R.string.toast_import_vault_failure), Toast.LENGTH_LONG).show()
            }
            else if(!cipherVersionSupported(getImportVaultActivity().jsonContent!!)) {
                Toast.makeText(activity, getString(R.string.toast_import_vault_failure_cipher_not_supported), Toast.LENGTH_LONG).show()
            }
            else {
                findNavController().navigate(R.id.action_importVault_LoadFileFragment_to_ImportFileFragment)
            }
        }
    }

    private fun cipherVersionSupported(jsonContent: JsonObject): Boolean {
        val cipherAlgorithm = ImportVaultUseCase.extractCipherAlgorithm(jsonContent)
        return Build.VERSION.SDK_INT >= cipherAlgorithm.supportedSdkVersion
    }

    private fun getImportVaultActivity() : ImportVaultActivity {
        return getBaseActivity() as ImportVaultActivity
    }

}