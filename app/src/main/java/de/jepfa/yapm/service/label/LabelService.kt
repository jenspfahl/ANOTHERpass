package de.jepfa.yapm.service.label

import android.content.Context
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.label.Label
import kotlin.random.Random

object LabelService {

    val defaultHolder = LabelsHolder()
    val externalHolder = LabelsHolder()

    fun getLabelFromEncLabel(key: SecretKeyHolder, encLabel: EncLabel): Label {
        val name = SecretService.decryptCommonString(key, encLabel.name)
        val desc = SecretService.decryptCommonString(key, encLabel.description)
        return Label(encLabel.id, name, desc, encLabel.color)
    }

    fun getEncLabelFromLabel(key: SecretKeyHolder, label: Label): EncLabel {
        val name = SecretService.encryptCommonString(key, label.name)
        val desc = SecretService.encryptCommonString(key, label.description)
        return EncLabel(label.labelId, null, name, desc, label.colorRGB)
    }


}