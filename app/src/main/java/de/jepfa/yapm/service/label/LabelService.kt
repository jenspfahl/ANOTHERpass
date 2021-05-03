package de.jepfa.yapm.service.label

import android.util.Log
import com.pchmn.materialchips.model.ChipInterface
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.label.LabelChip
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey
import kotlin.collections.HashSet

object LabelService {

    private const val ID_SEPARATOR = ","

    private val labelIdToCredentialIds: MutableMap<Int,MutableSet<Int>> = ConcurrentHashMap(16)
    private val nameToLabel: MutableMap<String, Label> = ConcurrentHashMap(16)
    private val idToLabel: MutableMap<Int, Label> = ConcurrentHashMap(16)

    data class Label(val encLabel: EncLabel, val labelChip: LabelChip) // TODO remove this and add id to LabelChip

    fun initLabels(key: SecretKey, encLabels: Set<EncLabel>) {
        encLabels
            .forEach {
                updateLabel(key, it)
            }
    }

    fun updateLabel(key: SecretKey, encLabel: EncLabel) {
        val encLabelId = encLabel.id
        if (encLabelId != null) {
            val labelChip = createLabelChip(key, encLabel)

            val label = Label(encLabel, labelChip)
            val existing = idToLabel.get(encLabelId)
            if (existing != null) {
                // important for name updates
                nameToLabel.remove(existing.labelChip.label)
            }
            nameToLabel.put(label.labelChip.label, label)
            idToLabel.put(encLabelId, label)
        }
    }

    fun removeLabel(label: Label) {
        nameToLabel.remove(label.labelChip.label)
        label.encLabel.id?.let {
            idToLabel.remove(it)
            labelIdToCredentialIds.remove(it)
        }
    }

    fun clearAll() {
        labelIdToCredentialIds.clear()
        nameToLabel.clear()
        idToLabel.clear()
    }

    fun updateLabelsForCredential(key: SecretKey, credential: EncCredential) {

        val labels = getLabelsForCredential(key, credential)

        labels
            .forEach { label ->
                val credentialId = credential.id
                val labelId = label.encLabel.id
                if (credentialId != null && labelId != null) {
                    val labelIdMapping = labelIdToCredentialIds.get(labelId)
                    if (labelIdMapping != null) {
                        labelIdMapping.add(credentialId)
                    } else {
                        val credentialIds = HashSet<Int>()
                        credentialIds.add(credentialId)
                        labelIdToCredentialIds.put(labelId, credentialIds)
                    }
                }
            }
    }

    fun getCredentialIdsForLabelId(labelId: Int): Set<Int>? {
        return labelIdToCredentialIds[labelId]
    }

    fun getAllLabels(): List<Label> {
        return nameToLabel.values
            .sortedBy { it.labelChip.label }
            .toList()
    }

    fun getAllLabelChips(): List<LabelChip> {
        return nameToLabel.values
            .map { it.labelChip }
            .sortedBy { it.label }
            .toList()
    }

    fun getLabelsForCredential(key: SecretKey, credential: EncCredential): List<Label> {
        if (!credential.isPersistent()) {
            return Collections.emptyList()
        }
        val labelsString = SecretService.decryptCommonString(key, credential.labels)
        val labels = stringIdsToLabels(labelsString)

        return labels
            .map { label ->
                val credentialId = credential.id
                val labelId = label.encLabel.id
                if (credentialId != null && labelId != null) {
                    lookupByLabelId(labelId)
                }
                else {
                    null
                }
            }
            .filterNotNull()
            .sortedBy { it.labelChip.label }
            .toList()

    }

    fun lookupByLabelName(labelName: String): Label? {
        return nameToLabel[labelName.toUpperCase()]
    }

    fun lookupByLabelId(id: Int): Label? {
        return idToLabel[id]
    }

    fun encryptLabelIds(key: SecretKey, chipList: List<ChipInterface>): Encrypted {
        val ids = chipList
            .map {lookupByLabelName(it.label)}
            .filterNotNull()
            .map { it.encLabel.id }
            .filterNotNull()
            .toSet()

        val idsAsString =  idSetToString(ids)
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
            Log.e("LIS", "cannot parse $ids ", e)
            return Collections.emptySet()
        }
    }

    private fun idSetToString(ids: Set<Int>): String {
        return ids.joinToString(separator = ID_SEPARATOR)
    }

    private fun createLabelChip(key: SecretKey, encLabel: EncLabel): LabelChip {
        val name = SecretService.decryptCommonString(key, encLabel.name)
        val desc = SecretService.decryptCommonString(key, encLabel.description)
        return LabelChip(encLabel.color, name, desc)
    }

}