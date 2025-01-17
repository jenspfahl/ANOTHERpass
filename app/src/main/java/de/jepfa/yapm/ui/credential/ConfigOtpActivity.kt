package de.jepfa.yapm.ui.credential

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.createLabelChip
import de.jepfa.yapm.util.getIntExtra
import de.jepfa.yapm.util.observeOnce
import java.util.*


class ConfigOtpActivity : SecureActivity() {


    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_otp)



        val saveButton: Button = findViewById(R.id.button_save)
        saveButton.setOnClickListener {

            finish()

        }

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Session.isDenied()) {
            return false
        }

        menuInflater.inflate(R.menu.menu_configure_otp, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return false
        }

        if (id == R.id.menu_delete_label) {


            return true
        }


        return super.onOptionsItemSelected(item)
    }


    override fun lock() {
        finish()
    }


}