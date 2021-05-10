package de.jepfa.yapm.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.createvault.CreateVaultEnterPinFragment

open class BaseFragment : Fragment() {

    protected var enableBack = false
    protected var backToPreviousFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getBaseActivity().supportActionBar?.setDisplayHomeAsUpEnabled(enableBack)
        setHasOptionsMenu(enableBack)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (enableBack && id == android.R.id.home) {
            if (backToPreviousFragment) {
                findNavController().navigateUp()
            }
            else {
                val upIntent = Intent(getBaseActivity().intent)
                getBaseActivity().navigateUpTo(upIntent)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun setTitle(titleId: Int) {
        getBaseActivity().setTitle(titleId)
    }


    fun getBaseActivity() : BaseActivity {
        return activity as BaseActivity
    }

    fun getApp(): YapmApp {
        return (activity as BaseActivity).application as YapmApp
    }
}