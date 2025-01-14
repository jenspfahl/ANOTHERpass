package de.jepfa.yapm.service.secret

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import de.jepfa.yapm.model.encrypted.KdfConfig
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password


object Argon2Service {

    private const val DEFAULT_ARGON2_PARALLELISM = 4

    private val argon2Kt = Argon2Kt()


    fun derive(password: Password, salt: Key, kdfConfig: KdfConfig): Key {

        val hashResult = argon2Kt.hash(
            mode = kdfConfig.kdf.argon2Mode ?: Argon2Mode.ARGON2_ID,
            password = password.toByteArray(),
            salt = salt.toByteArray(),
            tCostInIterations = kdfConfig.iterations,
            mCostInKibibyte = kdfConfig.memCostInMiB!! * 1024,
            parallelism = DEFAULT_ARGON2_PARALLELISM
        )

        return Key(hashResult.rawHashAsByteArray())
    }


}