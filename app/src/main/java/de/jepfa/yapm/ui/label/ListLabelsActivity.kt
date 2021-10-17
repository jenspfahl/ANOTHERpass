package de.jepfa.yapm.ui.label

import android.annotation.SuppressLint
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
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.AsyncWithProgressBar
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.usecase.LockVaultUseCase
import de.jepfa.yapm.usecase.label.DeleteUnusedLabelUseCase
import de.jepfa.yapm.util.toastText

class ListLabelsActivity : SecureActivity() {

    private lateinit var listLabelsAdapter: ListLabelsAdapter

    init {
        enableBack = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_labels)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        listLabelsAdapter = ListLabelsAdapter(this)
        recyclerView.adapter = listLabelsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        labelViewModel.allLabels.observe(this, { labels ->
            masterSecretKey?.let{ key ->
                LabelService.initLabels(key, labels.toSet()) //TODO bad solution, better just looking for updates
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

        menuInflater.inflate(R.menu.menu_label_list, menu)

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
        AsyncWithProgressBar(this,
            { DeleteUnusedLabelUseCase.execute(Unit, this) },
            { success ->
              if (!success) {
                  toastText(this, R.string.no_unused_credentials_to_delete)
                    //TODO better return number of deleted creds
              }
            })
    }

    override fun lock() {
       listLabelsAdapter.submitList(emptyList())
    }
}