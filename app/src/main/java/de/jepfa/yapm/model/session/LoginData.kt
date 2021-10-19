package de.jepfa.yapm.model.session

import de.jepfa.yapm.model.secret.Password

data class LoginData (val pin: Password, val masterPassword: Password)