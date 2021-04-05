package de.jepfa.yapm.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StringSetConverter {

    private val TYPE_OF_SET = object : TypeToken<Set<String?>?>() {}.type

    @TypeConverter
    fun restoreSet(listOfString: String?): Set<String> {
        return Gson().fromJson(
            listOfString,
            TYPE_OF_SET
        )
    }

    @TypeConverter
    fun saveSet(listOfString: Set<String?>?): String {
        return Gson().toJson(listOfString)
    }
}