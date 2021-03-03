package de.jepfa.yapm.ui.createvault

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Key
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.util.PreferenceUtil
import javax.crypto.SecretKey

class CreateVaultSummarizeFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_vault_summarize, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val key = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)

        val encPasswordBase64 = arguments?.getString(CreateVaultActivity.ARG_ENC_PASSWD)
        if (encPasswordBase64 == null) {
            Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
            return
        }

        val encPasswd = Encrypted.fromBase64String(encPasswordBase64)
        val passwd = Password("abcd") // TODO  SecretService.decryptPassword(key, encPasswd)

        val switchStorePasswd: Switch = view.findViewById(R.id.switch_store_master_password)

        val generatedPasswdView: TextView = view.findViewById(R.id.generated_passwd)
        generatedPasswdView.text = passwd.debugToString()

        view.findViewById<Button>(R.id.button_create_vault).setOnClickListener {

            val keyForMK = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MK)

            val salt = SecretService.getOrCreateSalt(getBaseActivity())

            val masterPin = extractAndStoreMasterPin(getBaseActivity(), salt)
            if (masterPin == null) {
                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val masterPassphrase = generateAndStoreMasterKey(getBaseActivity(), masterPin, passwd, salt, keyForMK)

            if (switchStorePasswd.isChecked) {
                val keyForMP = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP)
                val encPasswd = SecretService.encryptPassword(keyForMP, passwd)
                PreferenceUtil.put(PreferenceUtil.PREF_ENCRYPTED_MASTER_PASSWORD, encPasswd.toBase64String(), getBaseActivity())
            }

            masterPassphrase.clear()
            masterPin.clear()
            passwd.clear()

            findNavController().navigate(R.id.action_Create_Vault_to_ThirdFragment_to_Root)
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

    private fun generateAndStoreMasterKey(activity: BaseActivity, masterPin: Password, passwd: Password, salt: Key, keyForMK: SecretKey): Password {

        val masterPassphrase = SecretService.conjunctPasswords(masterPin, passwd, salt)
        val masterSK = SecretService.generateSecretKey(masterPassphrase, salt)

        val masterKey = SecretService.generateKey(128)
        val encryptedMasterKey = SecretService.encryptKey(masterSK, masterKey)

        val encEncryptedMasterKey = SecretService.encryptEncrypted(keyForMK, encryptedMasterKey)

        PreferenceUtil.put(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, encEncryptedMasterKey.toBase64String(), activity)
        return masterPassphrase
    }

    private fun extractAndStoreMasterPin(activity: BaseActivity, salt: Key): Password? {

        val keyForTemp = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)
        val keyForHPin = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_HPIN)

        val encPinBase64 = arguments?.getString(CreateVaultActivity.ARG_ENC_PIN)!!
        if (encPinBase64 == null) {
            return null
        }

        val encPin = Encrypted.fromBase64String(encPinBase64)
        val masterPin = SecretService.decryptPassword(keyForTemp, encPin)

        val hashedMasterPin = SecretService.hashPassword(masterPin, salt)
        val hashedEncMasterPin = SecretService.encryptKey(keyForHPin, hashedMasterPin)
        PreferenceUtil.put(PreferenceUtil.PREF_HASHED_MASTER_PIN, hashedEncMasterPin.toBase64String(), activity)
        return masterPin
    }

}