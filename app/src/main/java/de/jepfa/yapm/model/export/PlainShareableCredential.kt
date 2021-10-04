package de.jepfa.yapm.model.export

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.jepfa.yapm.model.secret.Password


data class PlainShareableCredential(val n: String,
                                    val aI: String,
                                    val u: String,
                                    val p: Password,
                                    val w: String,
) {
    constructor(jsonObject: JsonObject) :
            this(
                jsonObject.get(ATTRIB_NAME).asString,
                jsonObject.get(ATTRIB_ADDITIONAL_INFO).asString,
                jsonObject.get(ATTRIB_USER).asString,
                Password(jsonObject.get(ATTRIB_PASSWORD).asString),
                jsonObject.get(ATTRIB_WEBSITE).asString,
            )

    companion object {
        const val ATTRIB_NAME = "n"
        const val ATTRIB_ADDITIONAL_INFO = "aI"
        const val ATTRIB_USER = "u"
        const val ATTRIB_PASSWORD = "p"
        const val ATTRIB_WEBSITE = "w"

        fun fromJson(json: JsonElement): PlainShareableCredential {
            val jsonObject = json.asJsonObject
            return PlainShareableCredential(jsonObject)
        }
    }
}

