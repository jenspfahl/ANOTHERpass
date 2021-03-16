package de.jepfa.yapm.ui.qrcode

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Encrypted
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.ExtPermissionChecker
import de.jepfa.yapm.util.QRCodeUtil.generateQRCode

class QrCodeActivity : SecureActivity() {

    private val saveAsImage = 1

    private lateinit var head: String
    private lateinit var encQRC: Encrypted

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Secret.isDenied()) {
            return
        }

        setContentView(R.layout.activity_qr_code)

        val headTextView: TextView = findViewById(R.id.textview_head)
        val subTextView: TextView = findViewById(R.id.textview_subtext)
        val qrCodeImageView: ImageView = findViewById(R.id.imageview_qrcode)

        val encHead = Encrypted.fromBase64String(intent.getStringExtra(EXTRA_HEADLINE))
        val encSub = Encrypted.fromBase64String(intent.getStringExtra(EXTRA_SUBTEXT))
        encQRC = Encrypted.fromBase64String(intent.getStringExtra(EXTRA_QRCODE))
        val qrcColor = intent.getIntExtra(EXTRA_COLOR, Color.BLACK)

        val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)
        head = SecretService.decryptCommonString(tempKey, encHead)
        val sub = SecretService.decryptCommonString(tempKey, encSub)
        val qrc = SecretService.decryptPassword(tempKey, encQRC)

        headTextView.text = head
        subTextView.text = sub

        if (!qrc.isEmpty()) {
            val bitmap = generateQRCode(qrc.toString(), qrcColor)
            qrCodeImageView.setImageBitmap(bitmap)
            qrCodeImageView.setOnLongClickListener {
                AlertDialog.Builder(this)
                        .setTitle(head)
                        .setMessage(qrc.toString())
                        .show()
                true
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (Secret.isDenied()) {
            return false
        }

        menuInflater.inflate(R.menu.qrcode_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {

            navigateUpTo(intent)
            return true
        }


        if (Secret.isDenied()) {
            return false
        }

        if (id == R.id.menu_download_qrc) {
            val key = masterSecretKey
            if (key != null) {

                ExtPermissionChecker.verifyRWStoragePermissions(this) // TODO this is not enough

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/jpeg"
                intent.putExtra(Intent.EXTRA_TITLE, getFileName(head))
                startActivityForResult(Intent.createChooser(intent, "Save as"), saveAsImage)

            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == saveAsImage) {

            data?.data?.let {
                val qrcColor = intent.getIntExtra(EXTRA_COLOR, Color.BLACK)
                val intent = Intent(this, FileIOService::class.java)
                intent.action = FileIOService.ACTION_SAVE_QRC
                intent.putExtra(FileIOService.PARAM_FILE_URI, it)
                intent.putExtra(FileIOService.PARAM_QRC, encQRC.toBase64String())
                intent.putExtra(FileIOService.PARAM_QRC_COLOR, qrcColor)
                startService(intent)
            }

        }
    }

    private fun getFileName(head: String): String {
        return "${head}.jpeg"

    }

    override fun lock() {
        recreate()
    }

    companion object {
        const val EXTRA_HEADLINE = "head"
        const val EXTRA_SUBTEXT = "sub"
        const val EXTRA_QRCODE = "qrc"
        const val EXTRA_COLOR = "col"
    }
}