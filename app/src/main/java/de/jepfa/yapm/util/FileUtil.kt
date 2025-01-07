package de.jepfa.yapm.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import java.io.*


/**
 * Utils to work with files.
 *
 * @author Jens Pfahl
 */
object FileUtil {

    private const val MAX_FILE_SIZE_MB = 1024 * 1024 * 50


    /** Checks if external storage is available for read and write  */
    fun isExternalStorageWritable(): Boolean  {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    /** Checks if external storage is available to at least read  */
    fun isExternalStorageReadable(): Boolean  {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

    fun openInputStreamFromFile(context: Context, uri: Uri): InputStream? {

        val size = getFileSize(context, uri)?:0
        if (size > MAX_FILE_SIZE_MB) {
            Log.e(LOG_PREFIX + "READFILE", "File too big for $uri")
            return null
        }

        try {
            return context.contentResolver.openInputStream(uri)
        } catch (e: IOException) {
            Log.e(LOG_PREFIX + "READFILE", "Cannot read $uri", e)
            return null
        }

    }

    fun readFile(context: Context, uri: Uri): String? {

        val size = getFileSize(context, uri)?:0
        if (size > MAX_FILE_SIZE_MB) {
            Log.e(LOG_PREFIX + "READFILE", "File too big for $uri")
            return null
        }

        //Read text from file
        val text = StringBuilder()

        try {
            context.contentResolver.openInputStream(uri)?.use { `is` ->
                InputStreamReader(`is`).use { isr ->
                    BufferedReader(isr).use { br ->
                        var line: String?

                        do {
                            line = br.readLine()
                            if (line == null)
                                break
                            text.append(line)
                            text.append('\n')
                        } while (true)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(LOG_PREFIX + "READFILE", "Cannot read $uri", e)
            return null
        }

        return text.toString()
    }

    fun getFileSize(context: Context, uri: Uri): Long? {
        val fileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
        val fileSize = fileDescriptor?.length
        fileDescriptor?.close()
        return fileSize
    }


    fun getFileName(context: Context, uri: Uri): String? {
        val fileName: String?
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val idx = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx == null || idx == -1) {
            return null
        }
        fileName = cursor.getString(idx)
        cursor.close()

        return fileName
    }

    fun readBinaryFile(context: Context, uri: Uri): ByteArray? {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArrayOutputStream()

                var nRead: Int
                val data = ByteArray(16384)

                while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
                    buffer.write(data, 0, nRead)
                }

                return buffer.toByteArray()
            }
        } catch (e: IOException) {
            Log.e(LOG_PREFIX + "READFILE", "Cannot read $uri", e)
            return null
        }

        return null
    }

    fun writeFile(context: Context, uri: Uri, content: String): Boolean {

        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { osw ->
                    BufferedWriter(osw).use { bw -> bw.write(content) }
                }
            }
        } catch (e: IOException) {
            Log.e(LOG_PREFIX + "READFILE", "Cannot read $uri", e)
            return false
        }

        return true
    }

    fun writeFile(context: Context, uri: Uri, byteStream: ByteArrayOutputStream): Boolean {

        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                byteStream.writeTo(os)
            }
        } catch (e: IOException) {
            Log.e(LOG_PREFIX + "READFILE", "Cannot read $uri", e)
            return false
        }

        return true
    }



}
