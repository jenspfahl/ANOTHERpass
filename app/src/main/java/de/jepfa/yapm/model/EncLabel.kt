package de.jepfa.yapm.model

data class EncLabel(var name: Encrypted,
                    var color: Int?) {

    constructor(nameBase64: String, color: Int?) :
            this(Encrypted.fromBase64String(nameBase64), color)

    companion object {
        const val EXTRA_LABEL_NAME = "de.jepfa.yapm.ui.label.name"
        const val EXTRA_LABEL_COLOR = "de.jepfa.yapm.ui.label.color"

        const val ATTRIB_NAME = "name"
        const val ATTRIB_COLOR = "color"
    }
}