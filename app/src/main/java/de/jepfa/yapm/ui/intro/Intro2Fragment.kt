package de.jepfa.yapm.ui.intro

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.jepfa.yapm.R
import de.jepfa.yapm.service.secretgenerator.SecretStrength
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGeneratorSpec
import de.jepfa.yapm.util.PasswordColorizer


class Intro2Fragment : Fragment() {

    private lateinit var passphraseGenerator: PassphraseGenerator


    override fun onAttach(context: Context) {
        super.onAttach(context)
        passphraseGenerator = PassphraseGenerator(context = context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pseudoPasswdText = view.findViewById<TextView>(R.id.textview_pseudophrase)
        pseudoPasswdText.setOnClickListener {
            generateAndSetExamplePasswd(pseudoPasswdText)
        }
        generateAndSetExamplePasswd(pseudoPasswdText)
    }

    private fun generateAndSetExamplePasswd(pseudoPasswdText: TextView) {
        activity?.let { activity ->
            val generatedPassword =
                passphraseGenerator.generate(PassphraseGeneratorSpec(SecretStrength.NORMAL))
            var spannedString = PasswordColorizer.spannableString(generatedPassword, activity)
            pseudoPasswdText.text = spannedString
        }
    }

}