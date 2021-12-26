package de.jepfa.yapm.service.label

import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.label.Label

object LabelService {

    val defaultHolder = LabelsHolder()
    val externalHolder = LabelsHolder()

    fun createLabel(key: SecretKeyHolder, encLabel: EncLabel): Label {
        val name = SecretService.decryptCommonString(key, encLabel.name)
        val desc = SecretService.decryptCommonString(key, encLabel.description)
        return Label(encLabel.id, name, desc, encLabel.color)
    }
}