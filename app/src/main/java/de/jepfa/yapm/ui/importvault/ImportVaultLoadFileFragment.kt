package de.jepfa.yapm.ui.importvault

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.util.ExtPermissionChecker
import de.jepfa.yapm.util.FileUtil

class ImportVaultLoadFileFragment : BaseFragment() {

    val importVaultFile = 1

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_load_vault_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        getBaseActivity().supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val loadButton = view.findViewById<Button>(R.id.button_load_vault)
        loadButton.setOnClickListener {
            ExtPermissionChecker.verifyReadStoragePermissions(getBaseActivity())

            val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)
            val chooserIntent = Intent.createChooser(intent, getString(R.string.chooser_select_vault_file))
            startActivityForResult(chooserIntent, importVaultFile)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == importVaultFile) {
            data?.let {
                val selectedFile = data.data

                if (selectedFile != null && FileUtil.isExternalStorageReadable()) {
                    try {
                        val content = FileUtil.readFile(getBaseActivity(), selectedFile)
                        if (content != null) {
                            getImportVaultActivity().jsonContent = JsonParser.parseString(content).getAsJsonObject()
                        }
                    } catch (e: Exception) {
                        Log.e("RESTORE", "cannot import file $selectedFile", e)
                    }
                }
            }

            if (getImportVaultActivity().jsonContent == null) {
                Toast.makeText(activity, getString(R.string.toast_import_vault_failure), Toast.LENGTH_LONG).show()
            }
            else {
                findNavController().navigate(R.id.action_importVault_LoadFileFragment_to_ImportFileFragment)
            }
        }
    }

    private fun getImportVaultActivity() : ImportVaultActivity {
        return getBaseActivity() as ImportVaultActivity
    }

}