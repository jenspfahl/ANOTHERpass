package de.jepfa.yapm.ui.importcredentials

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.service.io.CsvService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.toastText

class ImportCredentialsLoadFileFragment : BaseFragment() {

    private val importCredentialsFile = 1

    init {
        enableBack = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_load_credentials_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)
        setTitle(R.string.import_credentials_load_file_fragment_label)

        val importCredentialsActivity = getImportCredentialsActivity()

        val loadButton = view.findViewById<Button>(R.id.button_load_credentials_file)
        loadButton.setOnClickListener {
            PermissionChecker.verifyReadStoragePermissions(importCredentialsActivity)
            if (PermissionChecker.hasReadStoragePermissions(importCredentialsActivity)) {
                val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.chooser_select_file_to_import))
                startActivityForResult(chooserIntent, importCredentialsFile)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val importCredentialsActivity = getImportCredentialsActivity()

        if (resultCode == RESULT_OK && requestCode == importCredentialsFile) {
            data?.let {
                val selectedFile = data.data

                if (selectedFile != null && FileUtil.isExternalStorageReadable()) {
                    val content = FileUtil.readFile(importCredentialsActivity, selectedFile)
                    if (content == null) {
                        toastText(importCredentialsActivity, R.string.cannot_parse_csv_credentials)
                        return
                    }
                    val csv = CsvService.parseCsv(content)
                    if (csv == null || csv.isEmpty()) {
                        toastText(importCredentialsActivity, R.string.cannot_parse_csv_credentials)
                        return
                    }
                    else {
                        val records = importCredentialsActivity.readContent(csv)
                        if (records == null || records.isEmpty()) {
                            toastText(importCredentialsActivity, R.string.cannot_parse_csv_credentials)
                            return
                        }
                        importCredentialsActivity.content = content
                        importCredentialsActivity.records = records
                        findNavController().navigate(R.id.action_importCredentials_LoadFileFragment_to_ImportFileFragment)
                    }
                }
            }
        }
    }


    private fun getImportCredentialsActivity() : ImportCredentialsActivity {
        return getBaseActivity() as ImportCredentialsActivity
    }

}