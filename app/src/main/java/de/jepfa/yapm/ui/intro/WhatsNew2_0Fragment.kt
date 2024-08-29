package de.jepfa.yapm.ui.intro

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.jepfa.yapm.R
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secretgenerator.SecretStrength
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGeneratorSpec
import de.jepfa.yapm.util.PasswordColorizer


class WhatsNew2_0Fragment : Fragment() {

    private lateinit var passphraseGenerator: PassphraseGenerator


    override fun onAttach(context: Context) {
        super.onAttach(context)
        passphraseGenerator = PassphraseGenerator(context = context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_whats_new_in_2_0, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val enableServer = view.findViewById<CheckBox>(R.id.enable_server)
        val currentText = enableServer.text

        enableServer.setOnCheckedChangeListener { _, checked ->
            PreferenceService.putBoolean(PreferenceService.PREF_SERVER_CAPABILITIES_ENABLED, checked, null)
            if (!checked) {
                enableServer.text = "Ok, the server capability is completelly deactivated. You can enable it again in the Settings."
            }
            else {
                enableServer.text = currentText
            }
        }

        val currentState = PreferenceService.getAsBool(PreferenceService.PREF_SERVER_CAPABILITIES_ENABLED, null)
        enableServer.isChecked = currentState
    }

}