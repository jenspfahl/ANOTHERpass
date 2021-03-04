package de.jepfa.yapm.ui.credential

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.CollapsingToolbarLayout
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory


class ShowCredentialActivity : SecureActivity() {

    val updateCredentialActivityRequestCode = 1

    private lateinit var credential: EncCredential


    private val credentialViewModel: CredentialViewModel by viewModels {
        CredentialViewModelFactory((application as YapmApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_credential)

        val toolbar: Toolbar = findViewById(R.id.activity_credential_detail_toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitle("") //TODO needed?

        val titleLayout: View = findViewById(R.id.collapsing_toolbar_layout_title)
        val subText = findViewById<TextView>(R.id.collapsing_toolbar_layout_title_subtext)
        titleLayout.post(Runnable {
            val layoutParams = toolbar.getLayoutParams() as CollapsingToolbarLayout.LayoutParams
            layoutParams.height = titleLayout.getHeight()
            toolbar.setLayoutParams(layoutParams)
        })

        val appBarLayout =
            findViewById<CollapsingToolbarLayout>(R.id.credential_detail_toolbar_layout)

        val additionalInfoTextView : TextView = findViewById(R.id.additional_info)
        val passwordTextView : TextView = findViewById(R.id.passwd)

        val idExtra = intent.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)


        credentialViewModel.getById(idExtra).observe(this, {
            credential = it
            val key = masterSecretKey
            if (key != null) {
                val name = SecretService.decryptCommonString(key, credential.name)
                val additionalInfo = SecretService.decryptCommonString(key, credential.additionalInfo)
                val password = SecretService.decryptPassword(key, credential.password)

                appBarLayout?.setTitle(name)
                additionalInfoTextView.setText(additionalInfo)
                passwordTextView.setText(password.debugToString())
            }
        })


        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.credential_detail_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            val upIntent = Intent(this, ListCredentialsActivity::class.java)
            navigateUpTo(upIntent)
            return true
        }

        if (id == R.id.menu_detach_credential) {

            DetachHelper.detachPassword(this, credential)

            return true
        }
        if (id == R.id.menu_change_credential) {

            val intent = Intent(this, NewOrChangeCredentialActivity::class.java)
            intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, credential.id)

            startActivityForResult(intent, updateCredentialActivityRequestCode)


            return true
        }

        if (id == R.id.menu_delete_credential) {

            val key = masterSecretKey
            if (key != null) {
                val decName = SecretService.decryptCommonString(key, credential.name)

                AlertDialog.Builder(this)
                        .setTitle(R.string.title_delete_credential)
                        .setMessage(getString(R.string.message_delete_credential, decName))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                            credentialViewModel.delete(credential)

                            val upIntent = Intent(this, ListCredentialsActivity::class.java)
                            navigateUpTo(upIntent)
                            true
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == updateCredentialActivityRequestCode && resultCode == Activity.RESULT_OK) {
            data?.let {

                var id: Int? = null
                val idExtra = it.getIntExtra(EncCredential.EXTRA_CREDENTIAL_ID, -1)
                if (idExtra != -1) {
                    id = idExtra
                }
                val nameBase64 = it.getStringExtra(EncCredential.EXTRA_CREDENTIAL_NAME)
                val additionalInfoBase64 = it.getStringExtra(EncCredential.EXTRA_CREDENTIAL_ADDITIONAL_INFO)
                val passwordBase64 = it.getStringExtra(EncCredential.EXTRA_CREDENTIAL_PASSWORD)

                val encName = Encrypted.fromBase64String(nameBase64)
                val encAdditionalInfo = Encrypted.fromBase64String(additionalInfoBase64)
                val encPassword = Encrypted.fromBase64String(passwordBase64)

                val credential = EncCredential(id, encName, encAdditionalInfo, encPassword)
                if (credential.isPersistent()) {
                    credentialViewModel.update(credential)
                }
            }
        }

    }

    override fun refresh(before: Boolean) {
        recreate()
    }
}