package de.jepfa.yapm.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

open class BaseFragment : Fragment() {

    protected var enableBack = false
    protected var backToPreviousFragment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getBaseActivity()?.supportActionBar?.setDisplayHomeAsUpEnabled(enableBack)
        setHasOptionsMenu(enableBack)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (enableBack && id == android.R.id.home) {
            if (backToPreviousFragment) {
                findNavController().navigateUp()
            }
            else {
                getBaseActivity()?.let {
                    val upIntent = Intent(it.intent)
                    it.navigateUpTo(upIntent)
                }
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun setTitle(titleId: Int) {
        getBaseActivity()?.setTitle(titleId)
    }


    fun getBaseActivity() : BaseActivity? {
        return activity as BaseActivity?
    }

    fun <T: BaseActivity> getBaseActivityAs() :T? {
        return activity as T?
    }

    fun getApp(): YapmApp {
        return (activity as BaseActivity).application as YapmApp
    }
}