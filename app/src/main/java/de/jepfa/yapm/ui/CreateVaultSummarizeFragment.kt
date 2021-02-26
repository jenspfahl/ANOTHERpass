package de.jepfa.yapm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Key
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
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

        val secretService = getApp().secretService
        val key = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_TEMP)

        val encPasswordBase64 = arguments?.getString(CreateVaultActivity.ARG_ENC_PASSWD)!!
        val encPasswd = Encrypted.fromBase64String(encPasswordBase64)
        val passwd = secretService.decryptPassword(key, encPasswd)

        val generatedPasswdView: TextView = view.findViewById(R.id.generated_passwd)
        generatedPasswdView.text = passwd.debugToString()

        view.findViewById<Button>(R.id.button_create_vault).setOnClickListener {

            val keyForMK = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_MK)

            val salt = secretService.getOrCreateSalt(activity as BaseActivity)

            val masterPin = extractAndStoreMasterPin(activity as BaseActivity, salt)

            val masterPassphrase = generateAndStoreMasterKey(activity as BaseActivity, masterPin, passwd, salt, keyForMK)

            masterPassphrase.clear()
            masterPin.clear()
            passwd.clear()

            findNavController().navigate(R.id.action_Create_Vault_to_ThirdFragment_to_Root)
        }
    }

    private fun generateAndStoreMasterKey(activity: BaseActivity, masterPin: Password, passwd: Password, salt: Key, keyForMK: SecretKey): Password {
        val secretService = getApp().secretService

        val masterPassphrase = secretService.conjunctPasswords(masterPin, passwd, salt)
        val masterSK = secretService.generateSecretKey(masterPassphrase, salt)

        val masterKey = secretService.generateKey(128)
        val encryptedMasterKey = secretService.encryptKey(masterSK, masterKey)

        val encEncryptedMasterKey = secretService.encryptEncrypted(keyForMK, encryptedMasterKey)

        PreferenceUtil.put(PreferenceUtil.PREF_ENCRYPTED_MASTER_KEY, encEncryptedMasterKey.toBase64String(), activity)
        return masterPassphrase
    }

    private fun extractAndStoreMasterPin(activity: BaseActivity, salt: Key): Password {
        val secretService = getApp().secretService

        val keyForTemp = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_TEMP)
        val keyForHPin = secretService.getAndroidSecretKey(secretService.ALIAS_KEY_HPIN)

        val encPinBase64 = arguments?.getString(CreateVaultActivity.ARG_ENC_PIN)!!
        val encPin = Encrypted.fromBase64String(encPinBase64)
        val masterPin = secretService.decryptPassword(keyForTemp, encPin)

        val hashedMasterPin = secretService.hashPassword(masterPin, salt)
        val hashedEncMasterPin = secretService.encryptKey(keyForHPin, hashedMasterPin)
        PreferenceUtil.put(PreferenceUtil.PREF_HASHED_MASTER_PIN, hashedEncMasterPin.toBase64String(), activity)
        return masterPin
    }

}