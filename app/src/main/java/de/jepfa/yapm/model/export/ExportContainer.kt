package de.jepfa.yapm.model.export

import com.google.gson.JsonElement
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.DebugInfo

val TYPE_ENC_CREDENTIAL_RECORD = "ECR"
val TYPE_ENC_CREDENTIAL_RECORD_V2 = "ECR2"
val TYPE_PLAIN_CREDENTIAL_RECORD = "PCR"

data class ExportContainer(val t: String, val c: Any) {
    companion object {
        const val ATTRIB_TYPE = "t"
        const val ATTRIB_CONTENT = "c"

        fun fromJson(json: JsonElement): ExportContainer? {
            return try {
                val jsonObject = json.asJsonObject
                val type = jsonObject.get(ATTRIB_TYPE).asString
                val contentAsJson = jsonObject.get(ATTRIB_CONTENT)
                val content = parseContent(type, contentAsJson)
                if (content == null) {
                    DebugInfo.logException("EXC", "cannot parse json container content object")
                    return null
                }
                ExportContainer(
                    type,
                    content
                )
            } catch (e: Exception) {
                DebugInfo.logException("EXC", "cannot parse json container", e)
                null
            }
        }

        @Throws
        private fun parseContent(type: String, json: JsonElement): Any? {
            return when (type) {
                TYPE_PLAIN_CREDENTIAL_RECORD ->
                    PlainShareableCredential.fromJson(json)
                TYPE_ENC_CREDENTIAL_RECORD ->
                    EncExportableCredential.fromJson(json)
                TYPE_ENC_CREDENTIAL_RECORD_V2 ->
                    Encrypted.fromBase64String(json.asString)
                else -> null
            }
        }
    }
}

