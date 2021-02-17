package de.jepfa.yapm.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.secretgenerator.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import de.jepfa.yapm.viewmodel.CredentialViewModel
import de.jepfa.yapm.viewmodel.CredentialViewModelFactory


class MainActivity : AppCompatActivity() {

    private val newCredentialActivityRequestCode = 1

    private val secretService = SecretService()

    private val credentialViewModel: CredentialViewModel by viewModels {
        CredentialViewModelFactory((application as YapmApp).repository)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = CredentialListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        credentialViewModel.allWords.observe(this, Observer { credentials ->
            credentials?.let { adapter.submitList(it) }
        })

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@MainActivity, NewCredentialActivity::class.java)
            startActivityForResult(intent, newCredentialActivityRequestCode)
        }
        /*
        val svc = Intent(this, OverlayShowingService::class.java)
        startService(svc)*/

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == newCredentialActivityRequestCode && resultCode == Activity.RESULT_OK) {
            data?.let {

                val nameBase64 = it.getStringExtra(NewCredentialActivity.EXTRA_CREDENTIAL_NAME)
                val additionalInfoBase64 = it.getStringExtra(NewCredentialActivity.EXTRA_CREDENTIAL_ADDITIONAL_INFO)
                val passwordBase64 = it.getStringExtra(NewCredentialActivity.EXTRA_CREDENTIAL_PASSWORD)

                val encName = Encrypted.fromBase64String(nameBase64)
                val encAdditionalInfo = Encrypted.fromBase64String(additionalInfoBase64)
                val encPassword = Encrypted.fromBase64String(passwordBase64)

                val credential = EncCredential(null, encName, encAdditionalInfo, encPassword)
                credentialViewModel.insert(credential)
            }
        }
    }

}