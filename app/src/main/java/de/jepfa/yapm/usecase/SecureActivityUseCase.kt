package de.jepfa.yapm.usecase

import de.jepfa.yapm.ui.SecureActivity

interface SecureActivityUseCase {
    fun execute(activity: SecureActivity): Boolean
}