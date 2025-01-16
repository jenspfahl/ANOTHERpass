package de.jepfa.yapm.service.label

import android.content.Context
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.label.LabelService.getLabelFromEncLabel
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.util.DebugInfo
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class LabelsHolder {

    private val ID_SEPARATOR = ","

    private val labelIdToCredentialIds: MutableMap<Int,MutableSet<Int>> = ConcurrentHashMap(16)
    private val nameToLabel: MutableMap<String, Label> = ConcurrentHashMap(16)
    private val idToLabel: MutableMap<Int, Label> = ConcurrentHashMap(16)

    fun initLabels(key: SecretKeyHolder, encLabels: Set<EncLabel>) {
        encLabels
            .forEach { encLabel ->
                encLabel.id?.let {
                    val label = getLabelFromEncLabel(key, encLabel)
                    updateLabel(label)
                }
            }
    }

    fun updateLabel(label: Label) {
        val labelId = label.labelId
        if (labelId != null) {
            val existing = idToLabel[labelId]
            if (existing != null) {
                // important for name updates
                nameToLabel.remove(existing.name)
            }
            nameToLabel[label.name] = label
            idToLabel[labelId] = label
        }
    }

    fun createNewLabel(labelName: String, context: Context, id: Int? = null): Label {
        val labelColors = context.resources.getIntArray(R.array.label_colors)
        labelColors.shuffle() // to get next color by random
        val allLabelColors = getAllLabels()
            .map { it.getColor(context) }
            .toSet()
        var freeColor = labelColors
            .filterNot { allLabelColors.contains(it) }
            .firstOrNull()

        if (freeColor == null) {
            val randId = Random.nextInt(allLabelColors.size)
            freeColor = allLabelColors.elementAt(randId)
        }

        return Label(id, labelName, freeColor, null)
    }

    fun removeLabel(label: Label) {
        nameToLabel.remove(label.name)
        label.labelId?.let {
            idToLabel.remove(it)
            labelIdToCredentialIds.remove(it)
        }
    }

    fun clearAll() {
        labelIdToCredentialIds.clear()
        nameToLabel.clear()
        idToLabel.clear()
    }

    fun updateLabelsForCredential(key: SecretKeyHolder, credential: EncCredential) {

        val labels = decryptLabelsForCredential(key, credential)

        labels
            .forEach { label ->
                val credentialId = credential.id
                val labelId = label.labelId
                if (credentialId != null && labelId != null) {
                    val labelIdMapping = labelIdToCredentialIds[labelId]
                    if (labelIdMapping != null) {
                        labelIdMapping.add(credentialId)
                    } else {
                        val credentialIds = HashSet<Int>()
                        credentialIds.add(credentialId)
                        labelIdToCredentialIds[labelId] = credentialIds
                    }
                }
            }
    }

    fun getCredentialIdsForLabelId(labelId: Int): Set<Int>? {
        return labelIdToCredentialIds[labelId]
    }

    fun getAllLabels(): List<Label> {
        return nameToLabel.values
            .sortedBy { it.name }
            .toList()
    }


    fun isEmpty() = nameToLabel.isEmpty()

    fun decryptLabelsForCredential(key: SecretKeyHolder, credential: EncCredential): List<Label> {
        val labelsString = SecretService.decryptCommonString(key, credential.labels)
        val labels = stringIdsToLabels(labelsString)

        return labels
            .map { label ->
                val labelId = label.labelId
                if (labelId != null) {
                    lookupByLabelId(labelId)
                }
                else {
                    null
                }
            }
            .filterNotNull()
            .sortedBy { it.name }
            .toList()

    }

    fun decryptLabelsIdsForCredential(key: SecretKeyHolder, credential: EncCredential): List<Int> {
        if (!credential.isPersistent()) {
            return Collections.emptyList()
        }
        val labelsString = SecretService.decryptCommonString(key, credential.labels)
        val labels = stringIdsToLabels(labelsString)

        return labels
            .map { it.labelId }
            .filterNotNull()
            .sorted()
            .toList()

    }

    fun lookupByLabelName(labelName: String): Label? {
        return nameToLabel[labelName.toUpperCase(Locale.ROOT)]
    }

    fun lookupByLabelId(id: Int): Label? {
        return idToLabel[id]
    }

    fun encryptLabelIds(key: SecretKeyHolder, labelNames: List<String>): Encrypted {
        val ids = labelNames
            .mapNotNull { lookupByLabelName(it) }
            .mapNotNull { it.labelId }
            .toSet()

        val idsAsString = idSetToString(ids)
        return SecretService.encryptCommonString(key, idsAsString)
    }

    private fun stringIdsToLabels(ids: String): Set<Label> {
        if (ids.isEmpty()) {
            return Collections.emptySet()
        }
        try {
            return ids.split(ID_SEPARATOR)
                .map { it.toInt() }
                .map { lookupByLabelId(it) }
                .filterNotNull()
                .toSet()
        } catch (e: NumberFormatException) {
            DebugInfo.logException("LIS", "cannot parse $ids ", e)
            return Collections.emptySet()
        }
    }

    private fun idSetToString(ids: Set<Int>): String {
        return ids.joinToString(separator = ID_SEPARATOR)
    }

    fun copyFrom(sourceHolder: LabelsHolder) {
        clearAll()

        sourceHolder.getAllLabels().forEach {
            updateLabel(it)
        }

    }


}