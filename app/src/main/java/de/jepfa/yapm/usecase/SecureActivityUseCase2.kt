package de.jepfa.yapm.usecase

import de.jepfa.yapm.ui.SecureActivity

interface SecureActivityUseCase2<INPUT> {
    fun execute(input: INPUT, activity: SecureActivity): Boolean
}