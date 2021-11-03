package de.jepfa.yapm.service.autofill

import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Key

object AutofillCredentialHolder {

    var currentCredential : EncCredential? = null
        private set
    var obfuscationKey: Key? = null
        private set



    fun update(credential: EncCredential, obfuscationKey: Key?) {
        clear()
        this.currentCredential = credential
        this.obfuscationKey = obfuscationKey
    }

    fun clear() {
        currentCredential = null
        obfuscationKey?.clear()
        obfuscationKey = null
    }

}