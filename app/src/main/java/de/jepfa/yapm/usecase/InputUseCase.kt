package de.jepfa.yapm.usecase

import de.jepfa.yapm.ui.BaseActivity

abstract class InputUseCase<INPUT, ACTIVITY: BaseActivity> : UseCase<INPUT, Unit, ACTIVITY> {

    final override fun execute(input: INPUT, activity: ACTIVITY): UseCaseOutput<Unit> {
        val success = doExecute(input, activity)
        return UseCaseOutput(success, Unit)
    }

    abstract fun doExecute(input: INPUT, activity: ACTIVITY): Boolean
}