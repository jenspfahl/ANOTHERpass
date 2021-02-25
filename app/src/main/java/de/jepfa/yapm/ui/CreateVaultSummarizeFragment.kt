package de.jepfa.yapm.ui

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Password
import java.util.*

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

        val encPasswd = arguments?.getString(CreateVaultActivity.ARG_ENC_PASSWD)
        val passwd = secretService.decryptPassword(key, encPasswd)

        val generatedPasswdView: TextView = view.findViewById(R.id.generated_passwd)
        generatedPasswdView.text = passwd.

        view.findViewById<Button>(R.id.button_create_vault).setOnClickListener {


            val encPin = arguments?.getString(CreateVaultActivity.ARG_ENC_PIN)

            //TODO create actual vault
            //    findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)

        }
    }

}