package de.jepfa.yapm.ui

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import de.jepfa.yapm.R

class NewCredentialActivity : AppCompatActivity() {
    private lateinit var editCredentialView: EditText

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_credential)
        editCredentialView = findViewById(R.id.edit_credential)

        val button = findViewById<Button>(R.id.button_save)
        button.setOnClickListener {
            val replyIntent = Intent()
            if (TextUtils.isEmpty(editCredentialView.text)) {
                setResult(Activity.RESULT_CANCELED, replyIntent)
            } else {
                val credential = editCredentialView.text.toString()
                replyIntent.putExtra(EXTRA_REPLY, credential)
                setResult(Activity.RESULT_OK, replyIntent)
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_REPLY = "com.example.android.credentiallistsql.REPLY"
    }

}