package de.jepfa.yapm.ui.importvault

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.util.PreferenceUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImportVaultImportFileFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_import_vault_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        getBaseActivity().supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val loadedFileStatusTextView = view.findViewById<TextView>(R.id.loaded_file_status)
        val mkTextView = view.findViewById<TextView>(R.id.text_scan_mk)

        val jsonContent = getImportVaultActivity().jsonContent
        if (jsonContent == null) {
            return
        }

        val createdAt = jsonContent.get(FileIOService.JSON_CREATION_DATE)?.asString
        val credentialsCount = jsonContent.get(FileIOService.JSON_CREDENTIALS_COUNT)?.asString
        val encMasterKey = jsonContent.get(FileIOService.JSON_ENC_MK)?.asString

        loadedFileStatusTextView.text = "Vault exported at $createdAt, ${System.lineSeparator()} contains $credentialsCount credentials"

        encMasterKey?.let {
            mkTextView.text = encMasterKey
        }

        val importButton = view.findViewById<Button>(R.id.button_import_loaded_vault)
        importButton.setOnClickListener {
            if (mkTextView.text.isEmpty() || encMasterKey == null) {
                Toast.makeText(getBaseActivity(), "Scan your master key first", Toast.LENGTH_LONG).show()
                false
            }


            val salt = jsonContent.get(FileIOService.JSON_VAULT_ID)?.asString
            salt?.let { PreferenceUtil.put(PreferenceUtil.PREF_SALT, salt, getBaseActivity()) }

            if (encMasterKey != null) {
                val keyForMK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)
                val encEncryptedMasterKey = SecretService.encryptEncrypted(keyForMK, Encrypted.fromBase64String(encMasterKey))

                PreferenceUtil.put(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, encEncryptedMasterKey.toBase64String(), getBaseActivity())
            }

            val credentialsJson = jsonContent.getAsJsonArray(FileIOService.JSON_CREDENTIALS)
            CoroutineScope(Dispatchers.IO).launch {
                credentialsJson
                        .map{ json -> deserializeCredential(json)}
                        .filterNotNull()
                        .forEach { c -> getApp().repository.insert(c) }
            }

            findNavController().navigate(R.id.action_import_Vault_to_Login)
        }
    }

    private fun deserializeCredential(json: JsonElement?): EncCredential? {
        if (json != null) {
            val jsonObject = json.asJsonObject
            return EncCredential(
                    jsonObject.get(EncCredential.ATTRIB_ID).asInt,
                    jsonObject.get(EncCredential.ATTRIB_NAME).asString,
                    jsonObject.get(EncCredential.ATTRIB_ADDITIONAL_INFO).asString,
                    jsonObject.get(EncCredential.ATTRIB_PASSWORD).asString,
                    jsonObject.get(EncCredential.ATTRIB_EXTRA_PIN_REQUIRED).asBoolean,
            )
        }
        else {
            return null
        }
    }

    private fun getImportVaultActivity() : ImportVaultActivity {
        return getBaseActivity() as ImportVaultActivity
    }

}