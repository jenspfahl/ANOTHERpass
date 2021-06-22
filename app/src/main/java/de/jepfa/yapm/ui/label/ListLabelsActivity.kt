package de.jepfa.yapm.ui.label

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.getIntExtra

class ListLabelsActivity : SecureActivity() {

    private lateinit var listLabelsAdapter: ListLabelsAdapter

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_labels)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        listLabelsAdapter = ListLabelsAdapter(this)
        recyclerView.adapter = listLabelsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        labelViewModel.allLabels.observe(this, { labels ->
            masterSecretKey?.let{ key ->
                LabelService.initLabels(key, labels.toSet())
                listLabelsAdapter.submitList(LabelService.getAllLabels())
            }
        })

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            val intent = Intent(this, EditLabelActivity::class.java)
            startActivity(intent)
        }
    }

    fun deleteLabel(label: LabelService.Label) {
        val key = masterSecretKey
        val labelId =label.encLabel.id
        if (key != null && labelId != null) {
            val credentialsToUpdate = LabelService.getCredentialIdsForLabelId(labelId)
            credentialsToUpdate?.forEach { credentialId ->
                credentialViewModel.getById(credentialId).observe(this, { credential ->
                    credential?.let {
                        val labels = LabelService.getLabelsForCredential(key, credential)

                        val remainingLabelChips = labels
                            .filterNot { it.encLabel.id == labelId}
                            .map { it.labelChip }
                        LabelService.encryptLabelIds(key, remainingLabelChips)
                        LabelService.updateLabelsForCredential(key, credential)
                    }
                })

            }
            LabelService.removeLabel(label)
            labelViewModel.delete(label.encLabel)
        }
    }

    override fun lock() {
       // listLabelsAdapter.submitList(emptyList())
    }
}