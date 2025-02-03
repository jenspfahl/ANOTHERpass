package de.jepfa.yapm.usecase

import de.jepfa.yapm.ui.BaseActivity

abstract class BasicUseCase<ACTIVITY: BaseActivity> : UseCase<Unit, Unit, ACTIVITY> {

    final override suspend fun execute(input: Unit, activity: ACTIVITY): UseCaseOutput<Unit> {
        return UseCaseOutput(execute(activity), Unit, null)
    }

    abstract fun execute(activity: ACTIVITY): Boolean
}