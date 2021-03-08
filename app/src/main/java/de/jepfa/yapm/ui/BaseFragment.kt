package de.jepfa.yapm.ui

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {

    fun getBaseActivity() : BaseActivity {
        return activity as BaseActivity
    }

    fun getApp(): YapmApp {
        return (activity as BaseActivity).application as YapmApp
    }
}