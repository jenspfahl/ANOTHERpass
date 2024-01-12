package de.jepfa.yapm.ui.intro

import android.os.Bundle
import android.webkit.WebView
import de.jepfa.yapm.R
import de.jepfa.yapm.ui.BaseActivity

class LicencesActivity : BaseActivity() {

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licences)

        title = getString(R.string.licences_title)
        val webView = findViewById<WebView>(R.id.licences_view)
        webView.loadUrl("file:///android_asset/open_source_licenses.html")
    }
}