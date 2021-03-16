package de.jepfa.yapm.usecase

import de.jepfa.yapm.ui.SecureActivity

interface UseCase {
    fun execute(activity: SecureActivity): Boolean
}