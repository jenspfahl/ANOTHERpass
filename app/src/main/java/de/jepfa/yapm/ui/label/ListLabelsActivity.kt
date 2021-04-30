package de.jepfa.yapm.ui.label

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncLabel
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.util.getIntExtra

class ListLabelsActivity : SecureActivity() {

    companion object {
        const val ADD_OR_CHANGE_LABEL_REQUEST_CODE = 1
    }

    private lateinit var listLabelsAdapter: ListLabelsAdapter

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_labels)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(enableBack)


        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        listLabelsAdapter = ListLabelsAdapter(this)
        recyclerView.adapter = listLabelsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        labelViewModel.allLabels.observe(this, { labels ->
            val key = masterSecretKey
            if (key != null) {
                LabelService.initLabels(key, labels.toSet())
                listLabelsAdapter.submitList(LabelService.getAllLabels())
            }
        })

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            val intent = Intent(this, EditLabelActivity::class.java)
            startActivityForResult(intent, ADD_OR_CHANGE_LABEL_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val key = masterSecretKey
        if (key != null && requestCode == ADD_OR_CHANGE_LABEL_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let {intent ->
                val id = intent.getIntExtra(EncLabel.EXTRA_LABEL_ID)
                val name = intent.getStringExtra(EncLabel.EXTRA_LABEL_NAME)
                val desc = intent.getStringExtra(EncLabel.EXTRA_LABEL_DESC) ?: ""
                val color = intent.getIntExtra(EncLabel.EXTRA_LABEL_COLOR)

                val encName = SecretService.encryptCommonString(key, name)
                val encDesc = SecretService.encryptCommonString(key, desc)
                val encLabel = EncLabel(id, encName, encDesc, color)

                if (encLabel.isPersistent()) {
                    labelViewModel.update(encLabel)
                }
                else {
                    labelViewModel.insert(encLabel)
                }
                LabelService.updateLabel(key, encLabel)
            }
        }

    }

    fun deleteLabel(label: LabelService.Label) {
        val key = masterSecretKey
        val labelId =label.encLabel.id
        if (key != null && labelId != null) {
            val credentialsToUpdate = LabelService.getCredentialIdsForLabelId(labelId)
            credentialsToUpdate?.forEach { credentialId ->
                val credential = credentialViewModel.getById(credentialId).value
                credential?.let {
                    val labels = LabelService.getLabelsForCredential(key, credential)

                    val remainingLabelChips = labels
                        .filterNot { it.encLabel.id == labelId}
                        .map { it.labelChip }
                    LabelService.encryptLabelIds(key, remainingLabelChips)
                    LabelService.updateLabelsForCredential(key, credential)
                }
            }
            LabelService.removeLabel(label)
            labelViewModel.delete(label.encLabel)
        }
    }

    override fun lock() {
       // TODO("Not yet implemented")
    }
}