package de.jepfa.yapm.ui.webextension

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity

class ListWebExtensionsActivity : SecureActivity() {

    private lateinit var listWebExtensionsAdapter: ListWebExtensionsAdapter

    init {
        enableBack = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_web_extensions)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        listWebExtensionsAdapter = ListWebExtensionsAdapter(this)
        recyclerView.adapter = listWebExtensionsAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        webExtensionViewModel.allWebExtensions.observe(this) { webExtensions ->
            masterSecretKey?.let { key ->
                val sorted = webExtensions
                    .filter { it.linked }
                    .sortedBy { SecretService.decryptCommonString(key, it.title).lowercase() }

                listWebExtensionsAdapter.submitList(sorted)
            }
        }
    }

    override fun lock() {
       listWebExtensionsAdapter.submitList(emptyList())
    }
}