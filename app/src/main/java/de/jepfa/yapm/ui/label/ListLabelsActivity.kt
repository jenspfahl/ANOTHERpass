package de.jepfa.yapm.ui.label

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.putEncryptedExtra

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (checkSession && Session.isDenied()) {
            return false
        }

        menuInflater.inflate(R.menu.list_labels_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (checkSession && Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return false
        }

        if (id == R.id.menu_delete_unused_labels) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_unused_labels))
                .setMessage(getString(R.string.message_delete_unused_labels))
                .setIcon(R.drawable.ic_baseline_label_24)
                .setPositiveButton(android.R.string.yes) { dialog, _ ->
                    deleteUnusedLabels()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
            return true
        }


        return super.onOptionsItemSelected(item)
    }

    private fun deleteUnusedLabels() {
        LabelService.getAllLabels()
            .forEach { label ->
                val labelId = label.labelId
                if (labelId != null) {
                    val notUsed =
                        LabelService.getCredentialIdsForLabelId(labelId)?.isEmpty() ?: true
                    if (notUsed) {
                        label.let {
                            deleteLabel(it)
                        }
                    }
                }
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