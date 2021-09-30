package de.jepfa.yapm.model.export

import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.Encrypted

data class EncExportableCredential(val i: Int?,
                                   val n: Encrypted,
                                   val aI: Encrypted,
                                   val u: Encrypted,
                                   val p: Encrypted,
                                   val w: Encrypted,
                                   val l: Encrypted,
                                   val o: Boolean,
) {

    constructor(credential: EncCredential) :
            this(
                credential.id,
                credential.name,
                credential.additionalInfo,
                credential.user,
                credential.password,
                credential.website,
                credential.labels,
                credential.isObfuscated,
            )


    fun toEncCredential(): EncCredential {
        return EncCredential(
            i,
            n,
            aI,
            u,
            p,
            null,
            w,
            l,
            o,
            null
        )
    }

}