package de.jepfa.yapm.ui.importcredentials

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.io.CsvService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.FileUtil.getFileName
import de.jepfa.yapm.util.PermissionChecker
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

        view.findViewById<TextView>(R.id.button_csv_columns_manually)?.setOnClickListener {

            val dialogBuilder = AlertDialog.Builder(importCredentialsActivity)

            val columnsContainer = LinearLayout(dialogBuilder.context)
            columnsContainer.orientation = LinearLayout.VERTICAL
            columnsContainer.setPadding(24)

            val scrollView = ScrollView(dialogBuilder.context)
            val height = (resources.displayMetrics.heightPixels * 0.45).toInt()
            scrollView.layoutParams =
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            scrollView.setPadding(0, 24, 0, 24)

            scrollView.addView(columnsContainer)

            val container = LinearLayout(dialogBuilder.context)
            container.orientation = LinearLayout.VERTICAL

            var message = getString(R.string.csv_column_message)
            if (!importCredentialsActivity.firstRecord.isNullOrEmpty()) {

                message = getString(R.string.csv_column_message_suggested_columns, importCredentialsActivity.fileName)

                val csvColumnsContainer = LinearLayout(context)
                csvColumnsContainer.orientation = LinearLayout.VERTICAL
                csvColumnsContainer.setPadding(32)

                val textView = TextView(importCredentialsActivity)
                textView.text = getString(R.string.current_csv_columns)
                csvColumnsContainer.addView(textView)

                val editView = EditText(importCredentialsActivity)
                editView.setSingleLine()
                editView.setText(importCredentialsActivity.firstRecord?.keys?.joinToString(separator = ", "))
                csvColumnsContainer.addView(editView)

                container.addView(csvColumnsContainer)
            }

            container.addView(scrollView)

            val nameField = addInputField(importCredentialsActivity, columnsContainer, getString(R.string.csv_column_credential_name), PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_NAME, importCredentialsActivity)
            val usernameField = addInputField(importCredentialsActivity, columnsContainer, getString(
                            R.string.csv_column_credential_username), PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_USERNAME, importCredentialsActivity)
            val websiteField = addInputField(importCredentialsActivity, columnsContainer, getString(
                            R.string.csv_column_credential_website), PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_WEBSITE, importCredentialsActivity)
            val passwordField = addInputField(importCredentialsActivity, columnsContainer, getString(
                            R.string.csv_column_credential_password), PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_PASSWORD, importCredentialsActivity)
            val expiryField = addInputField(importCredentialsActivity, columnsContainer, getString(R.string.csv_column_credential_expiry_date), PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_EXPIRY_DATE, importCredentialsActivity)
            val addInfoField = addInputField(importCredentialsActivity, columnsContainer, getString(
                            R.string.csv_column_credential_additional_info), PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_ADDITIONAL_INFO, importCredentialsActivity)


            val dialog = dialogBuilder
                .setTitle(getString(R.string.csv_column_title))
                .setMessage(message)
                .setView(container)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()


            dialog.setOnShowListener {
                val buttonPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                buttonPositive.setOnClickListener {
                    val nameColumn = validateAndNormalize(nameField) ?: return@setOnClickListener
                    val usernameColumn = validateAndNormalize(usernameField) ?: return@setOnClickListener
                    val websiteColumn = validateAndNormalize(websiteField) ?: return@setOnClickListener
                    val passwordColumn = validateAndNormalize(passwordField) ?: return@setOnClickListener
                    val expiryDateColumn = validateAndNormalize(expiryField) ?: return@setOnClickListener
                    val addInfoColumn = validateAndNormalize(addInfoField) ?: return@setOnClickListener

                    PreferenceService.putString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_NAME, nameColumn, importCredentialsActivity)
                    PreferenceService.putString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_ADDITIONAL_INFO, addInfoColumn, importCredentialsActivity)
                    PreferenceService.putString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_USERNAME, usernameColumn, importCredentialsActivity)
                    PreferenceService.putString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_WEBSITE, websiteColumn, importCredentialsActivity)
                    PreferenceService.putString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_PASSWORD, passwordColumn, importCredentialsActivity)
                    PreferenceService.putString(PreferenceService.DATA_CUSTOM_CSV_COLUMN_CREDENTIAL_EXPIRY_DATE, expiryDateColumn, importCredentialsActivity)
                    dialog.dismiss()

                    if (importCredentialsActivity.csvContent != null) {
                        // automatically process next step
                        processCsvContentAndMoveForward(importCredentialsActivity)
                    }
                }

                val buttonNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                buttonNegative.setOnClickListener {
                    dialog.dismiss()
                }
            }

            dialog.show()
        }
    }

    private fun validateAndNormalize(field: EditText): String? {
        val text = field.text.toString().trim().lowercase()
        if (text.isNotEmpty() && !text.matches(Regex("[a-z\\d_-]+"))) {
            field.error = getString(R.string.not_a_csv_column)
            field.requestFocus()
            return null
        }
        return text
    }

    private fun addInputField(
        importCredentialsActivity: ImportCredentialsActivity,
        columnsContainer: LinearLayout,
        label: String,
        prefKey: String,
        context: Context
    ): EditText {
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(0, 0, 0, 32)

        val textView = TextView(importCredentialsActivity)
        textView.text = label
        container.addView(textView)

        val editView = EditText(importCredentialsActivity)
        editView.setSingleLine()
        PreferenceService.getAsString(prefKey, context)?.let {
            editView.setText(it)
        }
        container.addView(editView)

        columnsContainer.addView(container)

        return editView
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val importCredentialsActivity = getImportCredentialsActivity()

        if (resultCode == RESULT_OK && requestCode == importCredentialsFile) {
            data?.let {
                val selectedFile = data.data

                if (selectedFile != null && FileUtil.isExternalStorageReadable()) {
                    importCredentialsActivity.fileName = getFileName(importCredentialsActivity, selectedFile)
                    val content = FileUtil.readFile(importCredentialsActivity, selectedFile)
                    if (content == null) {
                        toastText(importCredentialsActivity, R.string.cannot_read_file)
                        return
                    }
                    val csv = CsvService.parseCsv(content)
                    if (csv == null || csv.isEmpty()) {
                        toastText(importCredentialsActivity, R.string.empty_csv_credentials)
                        return
                    }
                    else {
                        importCredentialsActivity.csvContent = csv
                        importCredentialsActivity.content = content

                        processCsvContentAndMoveForward(importCredentialsActivity)
                    }
                }
            }
        }
    }


    private fun processCsvContentAndMoveForward(importCredentialsActivity: ImportCredentialsActivity) {
        val records = importCredentialsActivity.readContent(importCredentialsActivity.csvContent)
        if (records == null || records.isEmpty()) {
            toastText(importCredentialsActivity, R.string.cannot_parse_csv_credentials)
        }
        else {
            importCredentialsActivity.records = records
            findNavController().navigate(R.id.action_importCredentials_LoadFileFragment_to_ImportFileFragment)
        }
    }


    private fun getImportCredentialsActivity() : ImportCredentialsActivity {
        return getBaseActivity() as ImportCredentialsActivity
    }

}