package de.jepfa.yapm.service.io

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.google.gson.JsonObject
import com.opencsv.CSVReaderHeaderAware
import com.opencsv.CSVWriter
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.toSimpleDateFormat
import java.io.*
import java.util.*


object CsvService {

    fun parseCsv(content: String): List<Map<String, String>>? {
        val resultList = mutableListOf<Map<String, String>>()
        try {
            var line: MutableMap<String, String>?

            val reader = CSVReaderHeaderAware(StringReader(content))
            line = reader.readMap()
            while (line != null) {
                resultList.add(line)
                line = reader.readMap()
            }
        } catch (e: Exception) {
            Log.e("CSV", "cannot parse csvfile", e)
            return null
        }
        return resultList
    }

    fun writeCsvExportFile(context: Context, uri: Uri, data: String): Boolean {
        var success = false
        try {
            success = FileUtil.writeFile(context, uri, data)
            val content: String? = FileUtil.readFile(context, uri)
            if (TextUtils.isEmpty(content)) {
                //TODO this check seems not to work from time to time
                Log.e("BACKUP", "Empty file created: $uri")
                success = false
            }
        } catch (e: Exception) {
            Log.e("BACKUP", "Cannot write file $uri", e)
        }
        return success
    }

    fun createCsvExportContent(credentials: List<EncCredential>, secretKey: SecretKeyHolder?): String? {
        if (secretKey == null) {
            return null
        }
        try {
            val outputStream = ByteArrayOutputStream()
            val outputWriter = OutputStreamWriter(outputStream)
            val writer = CSVWriter(outputWriter)

            // adding header to csv
            val header = arrayOf("name", "url", "username", "password", "description", "labels", "expiresOn")
            writer.writeNext(header)

            // add data to csv
            credentials.forEach { encCredential ->
                val name = SecretService.decryptCommonString(secretKey, encCredential.name)
                val website = SecretService.decryptCommonString(secretKey, encCredential.website)
                val expiresAt = SecretService.decryptLong(secretKey, encCredential.expiresAt)
                val user = SecretService.decryptCommonString(secretKey, encCredential.user)
                val password = SecretService.decryptPassword(secretKey, encCredential.password)
                val additionalInfo = SecretService.decryptCommonString(secretKey, encCredential.additionalInfo)
                val labelsAsString = LabelService.defaultHolder.decryptLabelsForCredential(secretKey, encCredential)
                    .map { it.name }
                    .sorted()
                    .joinToString(separator = ",")
                val expiresAtAsString = if (expiresAt != null && expiresAt > 0) Date(expiresAt).toSimpleDateFormat() else ""

                val data = arrayOf(name, website, user, password.toRawFormattedPassword().toString(),
                    additionalInfo, labelsAsString, expiresAtAsString)
                writer.writeNext(data)
                password.clear()
            }

            writer.close()
            return outputStream.toString()
        } catch (e: IOException) {
            return null
        }
    }
}