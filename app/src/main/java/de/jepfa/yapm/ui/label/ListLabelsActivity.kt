package de.jepfa.yapm.ui.label

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.jepfa.yapm.R
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.SecureActivity

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

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            val intent = Intent(this, EditLabelActivity::class.java)
            startActivity(intent)
        }

        fab.setOnLongClickListener {
            it.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        view.x = event.getRawX() - (view.getWidth() / 2)
                        view.y= event.getRawY() - (view.getHeight())
                    }
                    MotionEvent.ACTION_UP -> view.setOnTouchListener(null)
                    else -> {
                    }
                }
                true
            }
            true
        }

    }

    fun deleteLabel(label: Label) {
        val key = masterSecretKey
        val labelId =label.labelId
        if (key != null && labelId != null) {
            val credentialsToUpdate = LabelService.getCredentialIdsForLabelId(labelId)
            credentialsToUpdate?.forEach { credentialId ->
                credentialViewModel.getById(credentialId).observe(this, { credential ->
                    credential?.let {
                        val labels = LabelService.getLabelsForCredential(key, credential)

                        val remainingLabelChips = labels
                            .filterNot { it.labelId == labelId}
                            .map { it.name }
                        LabelService.encryptLabelIds(key, remainingLabelChips)
                        LabelService.updateLabelsForCredential(key, credential)
                    }
                })

            }
            LabelService.removeLabel(label)
            LabelFilter.unsetFilterFor(label)
            labelViewModel.deleteById(label.labelId)
        }
    }

    override fun lock() {
       // TODO listLabelsAdapter.submitList(emptyList())
    }
}