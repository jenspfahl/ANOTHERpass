package de.jepfa.yapm.service.io

import java.util.Date
import java.util.UUID

data class CredentialFileRecord(
        val uuid: UUID?,
        val id: Int,
        val name: String,
        val url: String?,
        val userName: String?,
        val plainPassword: String,
        val description: String,
        val expiresAt: Date?,
        val modifiedAt: Date?,
        val labels: List<String>,
        val otpAuth: String?,
) {
        constructor(
                id: Int,
                name: String,
                url: String?,
                userName: String?,
                plainPassword: String,
                description: String,
                expiresOn: Date?,
                otpAuth: String?
                )
                : this(null, id, name, url, userName, plainPassword, description, expiresOn, null, emptyList(), otpAuth)
}