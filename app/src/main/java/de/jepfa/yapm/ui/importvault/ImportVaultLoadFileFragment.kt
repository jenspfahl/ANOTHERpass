package de.jepfa.yapm.ui.importvault

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseFragment

class ImportVaultLoadFileFragment : BaseFragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_load_vault_file, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        getBaseActivity().supportActionBar?.setDisplayHomeAsUpEnabled(false)

        val loadButton = view.findViewById<Button>(R.id.button_load_vault)
        loadButton.setOnClickListener {
            findNavController().navigate(R.id.action_importVault_LoadFileFragment_to_ImportFileFragment)
        }
    }

}