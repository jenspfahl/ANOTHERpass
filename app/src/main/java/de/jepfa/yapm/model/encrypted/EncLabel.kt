package de.jepfa.yapm.model.encrypted

import android.util.Log
import com.google.gson.JsonElement
import java.util.*

data class EncLabel(val id: Int?,
                    val uid: UUID? = UUID.randomUUID(),
                    override var name: Encrypted,
                    var description: Encrypted,
                    var color: Int?): EncNamed {

    constructor(id: Int?, uid: String?, nameBase64: String, descriptionBase64: String, color: Int?) :
            this(id,
                uid?.let { UUID.fromString(uid) },
                Encrypted.fromBase64String(nameBase64),
                Encrypted.fromBase64String(descriptionBase64), color)

    fun isPersistent(): Boolean {
        return id != null
    }

    companion object {
        const val EXTRA_LABEL_ID = "de.jepfa.yapm.ui.label.id"

        const val ATTRIB_ID = "id"
        const val ATTRIB_UID = "uid"
        const val ATTRIB_NAME = "name"
        const val ATTRIB_DESC = "description"
        const val ATTRIB_COLOR = "color"

        fun fromJson(json: JsonElement): EncLabel? {
            return try {
                val jsonObject = json.asJsonObject
                EncLabel(
                    jsonObject.get(ATTRIB_ID).asInt,
                    jsonObject.get(ATTRIB_UID)?.asString,
                    jsonObject.get(ATTRIB_NAME).asString,
                    jsonObject.get(ATTRIB_DESC).asString,
                    jsonObject.get(ATTRIB_COLOR)?.asInt
                )
            } catch (e: Exception) {
                Log.e("ENCL", "cannot parse json container", e)
                null
            }
        }
    }
}