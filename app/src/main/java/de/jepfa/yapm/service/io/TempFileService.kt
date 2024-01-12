package de.jepfa.yapm.service.io

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import java.io.File


object TempFileService {

    fun createTempImageContentUri(context: Context, bitmap: Bitmap, baseFileName: String): Uri? {
        try {
            val tempFile = createTempFile(context, "$baseFileName.jpeg")
            val success = FileIOService.bitmapToJpegFile(context.contentResolver, bitmap, tempFile.toUri())
            if (success) {
                return getContentUriFromFile(context, tempFile)
            }
        } catch (e: Exception) {
            Log.e(LOG_PREFIX + "TS", "cannot create file or content uri", e)
        }
        return null
    }

    fun getContentUriFromFile(context: Context, file: File): Uri? {
        try {
            return FileProvider.getUriForFile(context, "de.jepfa.yapm.fileprovider", file)
        } catch (e: Exception) {
            Log.e(LOG_PREFIX + "TS", "cannot create content uri", e)
        }
        return null
    }

    private const val SHARE_FOLDER = "shares"

    fun createTempFile(context: Context, fileName: String): File {
        val sharesPath = File(context.cacheDir, SHARE_FOLDER)
        sharesPath.mkdir()
        val tempFile = File(sharesPath, fileName)
        tempFile.deleteOnExit()
        return tempFile
    }

    fun clearSharesCache(context: Context) {
        try {
            val sharesPath = File(context.cacheDir, SHARE_FOLDER)
            sharesPath.deleteRecursively()
        } catch (e: Exception) {
            Log.e(LOG_PREFIX + "FS", "cannot clear shares cache", e)
        }
    }


}