package de.jepfa.yapm.model.encrypted

data class EncLabel(var id: Int?,
                    var name: Encrypted,
                    var description: Encrypted,
                    var color: Int?) {

    constructor(id: Int?, nameBase64: String, descriptionBase64: String, color: Int?) :
            this(id,
                Encrypted.fromBase64String(nameBase64),
                Encrypted.fromBase64String(descriptionBase64), color)

    fun isPersistent(): Boolean {
        return id != null
    }

    companion object {
        const val EXTRA_LABEL_ID = "de.jepfa.yapm.ui.label.id"

        const val ATTRIB_ID = "id"
        const val ATTRIB_NAME = "name"
        const val ATTRIB_DESC = "description"
        const val ATTRIB_COLOR = "color"

    }
}