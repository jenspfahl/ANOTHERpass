package de.jepfa.yapm.ui.importvault

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.zxing.integration.android.IntentIntegrator
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.EncLabel
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.usecase.CreateVaultUseCase
import de.jepfa.yapm.usecase.ImportVaultUseCase
import de.jepfa.yapm.usecase.LoginUseCase
import de.jepfa.yapm.util.AsyncWithProgressBar
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.QRCodeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ImportVaultImportFileFragment : BaseFragment() {

    private lateinit var mkTextView: TextView
    private var encMasterKey: String? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_import_vault_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        getBaseActivity().supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        val loadedFileStatusTextView = view.findViewById<TextView>(R.id.loaded_file_status)
        val scanQrCodeImageView = view.findViewById<ImageView>(R.id.imageview_scan_qrcode)

        mkTextView = view.findViewById<TextView>(R.id.text_scan_mk)

        val jsonContent = getImportVaultActivity().jsonContent
        if (jsonContent == null) {
            return
        }

        val createdAt = jsonContent.get(FileIOService.JSON_CREATION_DATE)?.asString
        val credentialsCount = jsonContent.get(FileIOService.JSON_CREDENTIALS_COUNT)?.asString
        val labelsCount = jsonContent.get(FileIOService.JSON_LABELS_COUNT)?.asString
        encMasterKey = jsonContent.get(FileIOService.JSON_ENC_MK)?.asString

        loadedFileStatusTextView.text = "Vault exported at $createdAt, ${System.lineSeparator()} contains $credentialsCount credentials and ${labelsCount} labels"

        encMasterKey?.let {
            mkTextView.text = encMasterKey
        }

        scanQrCodeImageView.setOnClickListener {
            QRCodeUtil.scanQRCode(this, "Scanning Encrypted Master KEy")
            true
        }

        val importButton = view.findViewById<Button>(R.id.button_import_loaded_vault)
        importButton.setOnClickListener {
            if (mkTextView.text.isEmpty() || encMasterKey == null) {
                Toast.makeText(getBaseActivity(), "Scan your master key first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            importVault(jsonContent, encMasterKey)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {

            findNavController().navigate(R.id.action_importVault_ImportFileFragment_to_LoadFileFragment)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val scanned = result.contents

            if (scanned.startsWith(Encrypted.TYPE_ENC_MASTER_KEY)) {
                encMasterKey = scanned
                mkTextView.setText(encMasterKey)
            }
            else {
                Toast.makeText(getBaseActivity(), "This is not an encrypted master key.", Toast.LENGTH_LONG).show()
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
        encMasterKey: String?
    ) {

        getBaseActivity().getProgressBar()?.let {

            AsyncWithProgressBar(
                getBaseActivity(),
                {
                    val success = ImportVaultUseCase.execute(jsonContent, encMasterKey, getBaseActivity())
                    success
                },
                { success ->
                    if (!success) {
                        Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    }
                    else {
                        findNavController().navigate(R.id.action_importVault_to_Login)
                        getBaseActivity().finishAffinity()
                    }
                }
            )
        }
    }

}