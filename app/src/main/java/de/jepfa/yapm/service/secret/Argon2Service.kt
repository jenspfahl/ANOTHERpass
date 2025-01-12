package de.jepfa.yapm.service.secret

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password


object Argon2Service {

    val ARGON_2_MODE = Argon2Mode.ARGON2_ID
    const val DEFAULT_ARGON2_ITERATIONS = 5
    const val DEFAULT_ARGON2_KIB = 65536
    const val DEFAULT_ARGON2_PARALELISM = 4

    private val argon2Kt = Argon2Kt()


    fun derive(password: Password, salt: Key): Key {

        val hashResult = argon2Kt.hash(
            mode = ARGON_2_MODE,
            password = password.toByteArray(),
            salt = salt.toByteArray(),
            tCostInIterations = DEFAULT_ARGON2_ITERATIONS,
            mCostInKibibyte = DEFAULT_ARGON2_KIB,
            parallelism = DEFAULT_ARGON2_PARALELISM
        )

        return Key(hashResult.rawHashAsByteArray())
    }


}