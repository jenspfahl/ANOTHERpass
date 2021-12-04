package de.jepfa.yapm.model.session

import de.jepfa.yapm.model.Clearable
import de.jepfa.yapm.model.secret.Password

data class LoginData (val pin: Password, val masterPassword: Password) : Clearable {
    override fun clear() {
        pin.clear()
        masterPassword.clear()
    }
}