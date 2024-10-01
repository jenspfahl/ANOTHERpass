package de.jepfa.yapm.ui.intro

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.jepfa.yapm.R
import de.jepfa.yapm.service.secretgenerator.passphrase.PassphraseGenerator


class Intro6Fragment : Fragment() {

    private lateinit var passphraseGenerator: PassphraseGenerator


    override fun onAttach(context: Context) {
        super.onAttach(context)
        passphraseGenerator = PassphraseGenerator(context = context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro_6, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val textView: TextView = view.findViewById(R.id.textview_subtext)
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

}