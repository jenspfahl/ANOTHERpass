package de.jepfa.yapm.service.autofill

import de.jepfa.yapm.model.encrypted.EncCredential

object CurrentCredentialHolder {

    var currentCredential : EncCredential? = null
    //TODO add obfuscationKEy and use it on demand
}