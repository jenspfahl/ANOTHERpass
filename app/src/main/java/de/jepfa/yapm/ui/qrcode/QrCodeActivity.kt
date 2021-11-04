package de.jepfa.yapm.ui.qrcode

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.service.io.TempFileService
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.PermissionChecker
import de.jepfa.yapm.util.QRCodeUtil.generateQRCode
import de.jepfa.yapm.util.getEncryptedExtra
import de.jepfa.yapm.util.putEncryptedExtra
import de.jepfa.yapm.util.toastText
import java.util.*


class QrCodeActivity : SecureActivity() {

    private val saveAsImage = 1

    private lateinit var head: String
    private lateinit var encQRC: Encrypted
    private var bitmap: Bitmap? = null

    init {
        enableBack = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        checkSession = !intent.getBooleanExtra(EXTRA_NO_SESSION_CHECK, false)

        super.onCreate(savedInstanceState)

        if (checkSession && Session.isDenied()) {
            return
        }

        setContentView(R.layout.activity_qr_code)

        val headTextView: TextView = findViewById(R.id.textview_head)
        val subTextView: TextView = findViewById(R.id.textview_subtext)
        val qrCodeImageView: ImageView = findViewById(R.id.imageview_qrcode)

        val encHead = intent.getEncryptedExtra(EXTRA_HEADLINE, Encrypted.empty())
        val encSub = intent.getEncryptedExtra(EXTRA_SUBTEXT, Encrypted.empty())
        val encQrcHeader = intent.getEncryptedExtra(EXTRA_QRCODE_HEADER, Encrypted.empty())
        encQRC = intent.getEncryptedExtra(EXTRA_QRCODE, Encrypted.empty())
        val qrcColor = intent.getIntExtra(EXTRA_COLOR, Color.BLACK)

        val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
        head = SecretService.decryptCommonString(tempKey, encHead)
        val sub = SecretService.decryptCommonString(tempKey, encSub)
        val qrcHeader = SecretService.decryptCommonString(tempKey, encQrcHeader)
        val qrc = SecretService.decryptPassword(tempKey, encQRC)

        headTextView.text = head
        subTextView.text = sub

        if (!qrc.isEmpty()) {
            bitmap = generateQRCode(qrcHeader, qrc.toRawFormattedPassword(), qrcColor, this)
            qrCodeImageView.setImageBitmap(bitmap)
            qrCodeImageView.setOnLongClickListener {
                AlertDialog.Builder(this)
                        .setTitle(head)
                        .setMessage(qrc.toRawFormattedPassword() + System.lineSeparator() + "Size: ${qrc.length}")
                        .show()
                true
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (checkSession && Session.isDenied()) {
            return false
        }

        menuInflater.inflate(R.menu.menu_qrcode, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val nfcItem = menu?.findItem(R.id.menu_download_as_nfc)
        if (nfcItem != null && !NfcService.isNfcAvailable(this)) {
            nfcItem.isVisible = false
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (checkSession && Session.isDenied()) {
            LockVaultUseCase.execute(this)
            return false
        }

        if (id == R.id.menu_download_qrc) {
            PermissionChecker.verifyRWStoragePermissions(this)
            if (PermissionChecker.hasRWStoragePermissions(this)) {

                AlertDialog.Builder(this)
                    .setTitle(R.string.qrc_download_as_img)
                    .setMessage(R.string.hint_store_qr_as_file)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.title_continue) { dialog, _ ->
                        dialog.dismiss()
                        val downloadIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        downloadIntent.addCategory(Intent.CATEGORY_OPENABLE)
                        downloadIntent.type = "image/jpeg"
                        downloadIntent.putExtra(Intent.EXTRA_TITLE, getFileName(head))
                        startActivityForResult(Intent.createChooser(downloadIntent, getString(R.string.save_as)), saveAsImage)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

            }
        }

        if (id == R.id.menu_share_as_qrc) {
            bitmap?.let { bitmap ->
                val contentUri = TempFileService.createTempImageContentUri(this, bitmap, getBaseFileName(head))
                if (contentUri != null) {
                    val shareIntent = Intent()
                    shareIntent.action = Intent.ACTION_SEND
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, head)
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.send_to)))
                }
                else {
                    toastText(this, R.string.cannot_share_qr_code)
                }
            }
        }

        if (id == R.id.menu_download_as_nfc) {
            val withAppRecord = intent.getBooleanExtra(EXTRA_NFC_WITH_APP_RECORD, false)
            val noSessionCheck = intent.getBooleanExtra(EXTRA_NO_SESSION_CHECK, false)

            val nfcIntent = Intent(this, NfcActivity::class.java)
            nfcIntent.putExtra(NfcActivity.EXTRA_MODE, NfcActivity.EXTRA_MODE_RW)
            nfcIntent.putExtra(NfcActivity.EXTRA_WITH_APP_RECORD, withAppRecord)
            nfcIntent.putExtra(NfcActivity.EXTRA_NO_SESSION_CHECK, noSessionCheck)
            nfcIntent.putEncryptedExtra(NfcActivity.EXTRA_DATA, encQRC)
            startActivity(nfcIntent)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun getBaseFileName(head: String): String {
        if (head.isBlank()) {
            return UUID.randomUUID().toString()
        }
        return head
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == saveAsImage) {

            data?.data?.let {
                val qrcColor = intent.getIntExtra(EXTRA_COLOR, Color.BLACK)
                val header = intent.getStringExtra(EXTRA_QRCODE_HEADER)
                val intent = Intent(this, FileIOService::class.java)
                intent.action = FileIOService.ACTION_SAVE_QRC
                intent.putExtra(FileIOService.PARAM_FILE_URI, it)
                intent.putExtra(FileIOService.PARAM_QRC, encQRC.toBase64String())
                intent.putExtra(FileIOService.PARAM_QRC_HEADER, header)
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
        const val EXTRA_QRCODE_HEADER = "qrc_header"
        const val EXTRA_COLOR = "col"
        const val EXTRA_NFC_WITH_APP_RECORD = "nfcWithAppRecord"
        const val EXTRA_NO_SESSION_CHECK = "noSessionCheck"
    }
}