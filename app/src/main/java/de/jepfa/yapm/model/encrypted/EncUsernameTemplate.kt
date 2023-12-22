package de.jepfa.yapm.model.encrypted

import android.util.Log
import com.google.gson.JsonElement
import java.util.*

data class EncUsernameTemplate(val id: Int?,
                               var username: Encrypted,
                               var generatorType: Encrypted,
                               ) {

    enum class GeneratorType {
        NONE,
        EMAIL_EXTENSION_CREDENTIAL_NAME_BASED,
        EMAIL_EXTENSION_CREDENTIAL_RANDOM_BASED,
    }

    constructor(id: Int?, usernameBase64: String, generatorTypeBase64: String) :
            this(id,
                Encrypted.fromBase64String(usernameBase64),
                Encrypted.fromBase64String(generatorTypeBase64))

    fun isPersistent(): Boolean {
        return id != null
    }

    companion object {
        const val EXTRA_USERNAME_TEMPLATE_ID = "de.jepfa.yapm.ui.username_template.id"

        const val ATTRIB_ID = "id"
        const val ATTRIB_USERNAME = "username"
        const val ATTRIB_GENERATOR_TYPE = "generatorType"

        fun fromJson(json: JsonElement): EncUsernameTemplate? {
            return try {
                val jsonObject = json.asJsonObject
                EncUsernameTemplate(
                    jsonObject.get(ATTRIB_ID).asInt,
                    jsonObject.get(ATTRIB_USERNAME).asString,
                    jsonObject.get(ATTRIB_GENERATOR_TYPE).asString,
                )
            } catch (e: Exception) {
                Log.e("ENCL", "cannot parse json container", e)
                null
            }
        }
    }
}