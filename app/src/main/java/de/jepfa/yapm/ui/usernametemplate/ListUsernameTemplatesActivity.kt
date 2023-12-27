package de.jepfa.yapm.ui.usernametemplate

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import de.jepfa.yapm.R
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity

class ListUsernameTemplatesActivity : SecureActivity() {

    private lateinit var listUsernameTemplatesAdapter: ListUsernameTemplatesAdapter

    init {
        enableBack = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_username_templates)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        listUsernameTemplatesAdapter = ListUsernameTemplatesAdapter(this)
        recyclerView.adapter = listUsernameTemplatesAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        usernameTemplateViewModel.allUsernameTemplates.observe(this) { usernameTemplates ->
            masterSecretKey?.let { key ->
                val sorted = usernameTemplates.sortedBy { SecretService.decryptCommonString(key, it.username).lowercase() }
                listUsernameTemplatesAdapter.submitList(sorted)
            }
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            val intent = Intent(this, EditUsernameTemplateActivity::class.java)
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

    override fun lock() {
       listUsernameTemplatesAdapter.submitList(emptyList())
    }
}