package de.jepfa.yapm.service.io

import android.content.Context
import android.net.Uri
import android.util.Log
import com.opencsv.CSVReaderHeaderAware
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.util.FileUtil
import java.io.StringReader


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

}