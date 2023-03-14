package de.jepfa.yapm.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import de.jepfa.yapm.R
import de.jepfa.yapm.service.notification.NotificationService


class PasteContentProvider : ContentProvider() {

    companion object {
        val contentUri: Uri = Uri.parse("content://de.jepfa.yapm.paste_checker")

        var enablePushNotification = false
    }


    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun getType(uri: Uri): String {
        if (enablePushNotification) {
            context?.let {
                NotificationService.pushNotification(
                    it,
                    NotificationService.CHANNEL_ID_PASTE,
                    it.getString(R.string.test_copypaste_password), it.getString(R.string.password_pasted_by_another),
                    NotificationService.NOTIFICATION_ID_PASTE_SUCCESS
                )
            }
        }
        return "text/plain"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun onCreate(): Boolean {
        context?.let {
            NotificationService.createNotificationChannel(
                it,
                NotificationService.CHANNEL_ID_PASTE,
                it.getString(R.string.notification_channel_paste_title)
            )
        }

        return true
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor {
        return populateCursor()
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return 0
    }

    private fun populateCursor(): Cursor {
        val cursor = MatrixCursor(arrayOf("pwd"))
        cursor.addRow(arrayOf("testpwd"))

        return cursor
    }
}