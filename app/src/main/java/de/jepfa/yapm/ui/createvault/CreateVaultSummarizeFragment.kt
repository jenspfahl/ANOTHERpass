package de.jepfa.yapm.ui.createvault

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secret.SecretService.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.service.secret.SecretService.decryptPassword
import de.jepfa.yapm.service.secret.SecretService.encryptPassword
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_MASTER_PASSWD
import de.jepfa.yapm.usecase.CreateVaultUseCase
import de.jepfa.yapm.usecase.ExportEncMasterPasswordUseCase
import de.jepfa.yapm.usecase.LoginUseCase
import de.jepfa.yapm.util.AsyncWithProgressBar
import de.jepfa.yapm.util.PasswordColorizer
import de.jepfa.yapm.util.getEncrypted

class CreateVaultSummarizeFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_vault_summarize, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val encMasterPasswd = arguments?.getEncrypted(ARG_ENC_MASTER_PASSWD)
        if (encMasterPasswd == null) {
            Log.e("CV", "No master passwd in extra")
            Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
            return
        }
        val transSK = getAndroidSecretKey(ALIAS_KEY_TRANSPORT)
        val masterPasswd = decryptPassword(transSK, encMasterPasswd)

        val switchStorePasswd: Switch = view.findViewById(R.id.switch_store_master_password)
        val generatedPasswdView: TextView = view.findViewById(R.id.generated_passwd)
        generatedPasswdView.text = PasswordColorizer.spannableString(masterPasswd, getBaseActivity())

        val exportAsQrcImageView: ImageView = view.findViewById(R.id.imageview_qrcode)
        exportAsQrcImageView.setOnClickListener {
            val tempKey = getAndroidSecretKey(ALIAS_KEY_TRANSPORT)
            val encMasterPasswd = encryptPassword(tempKey, masterPasswd)
            ExportEncMasterPasswordUseCase.execute(encMasterPasswd, true, getBaseActivity())
        }

        view.findViewById<Button>(R.id.button_create_vault).setOnClickListener {

            val encPin = arguments?.getEncrypted(CreateVaultActivity.ARG_ENC_PIN)
            if (encPin == null) {
                Log.e("CV", "No pin in extra")
                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val pin = decryptPassword(transSK, encPin)

            createVault(pin, masterPasswd, switchStorePasswd.isChecked)

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(getBaseActivity(), CreateVaultEnterPassphraseFragment::class.java)
            getBaseActivity().navigateUpTo(upIntent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createVault(
        pin: Password,
        masterPasswd: Password,
        storeMasterPassword: Boolean
    ) {

        getBaseActivity().getProgressBar()?.let {

            AsyncWithProgressBar(
                getBaseActivity(),
                {
                    val success = CreateVaultUseCase.execute(pin, masterPasswd, storeMasterPassword, getBaseActivity())
                    if (success) {
                        LoginUseCase.execute(pin, masterPasswd, getBaseActivity())
                    }
                    success
                },
                { success ->
                    if (!success) {
                        Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                    }
                    else {
                        LoginUseCase.execute(pin, masterPasswd, getBaseActivity())
                        pin.clear()
                        masterPasswd.clear()
                        findNavController().navigate(R.id.action_Create_Vault_to_ThirdFragment_to_Root)
                        getBaseActivity().finish()
                    }
                }
            )

        }
    }
}


