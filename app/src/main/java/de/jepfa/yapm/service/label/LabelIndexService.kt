package de.jepfa.yapm.service.label

import android.util.Log
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.EncLabel
import de.jepfa.yapm.model.Encrypted
import java.lang.NumberFormatException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashSet

object LabelIndexService {

    private val labelIdToCredentialIds: MutableMap<Int,MutableSet<Int>> = ConcurrentHashMap(16)
    private val encLabelToId: MutableMap<Encrypted, Int> = ConcurrentHashMap(16)
    private val idToEncLabel: MutableMap<Int, EncLabel> = ConcurrentHashMap(16)

    fun init(encLabels: Set<EncLabel>) {

        encLabels
            .forEach {
                update(it)
            }
    }

    fun update(encLabel: EncLabel) {
        val encLabelId = encLabel.id
        if (encLabelId != null) {
            encLabelToId.put(encLabel.name, encLabelId)
            idToEncLabel.put(encLabelId, encLabel)
        }
    }

    fun stringToIdSet(ids: String): Set<EncLabel> {
        if (ids.isEmpty()) {
            return Collections.emptySet()
        }
        try {
            return ids.split(",")
                .map { it.toInt() }
                .map { lookupLabelId(it) }
                .filterNotNull()
                .toSet()
        } catch (e: NumberFormatException) {
            Log.e("LIS", "cannot parse $ids ", e)
            return Collections.emptySet()
        }
    }

    fun update(credential: EncCredential, labelIds: Set<Int>) {
        labelIds
            .forEach { labelId ->
                val credentialId = credential.id
                if (credentialId != null) {
                    val labelIdMapping = labelIdToCredentialIds.get(labelId)
                    if (labelIdMapping != null) {
                        labelIdMapping + credentialId
                    } else {
                        labelIdToCredentialIds.put(labelId, HashSet(credentialId))
                    }
                }
            }
    }

    fun lookupLabelId(encLabelName: Encrypted): Int? {
        return encLabelToId[encLabelName]
    }

    fun lookupLabelId(id: Int): EncLabel? {
        return idToEncLabel[id]
    }

    fun idSetToString(ids: Set<Int>): String {
        return ids.joinToString(separator = ",")
    }

}