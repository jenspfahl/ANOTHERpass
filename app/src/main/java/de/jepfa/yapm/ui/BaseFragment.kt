package de.jepfa.yapm.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import de.jepfa.yapm.service.secretgenerator.PasswordStrength

open class BaseFragment : Fragment() {


    fun getApp(): YapmApp {
        return (activity as BaseActivity).application as YapmApp
    }
}