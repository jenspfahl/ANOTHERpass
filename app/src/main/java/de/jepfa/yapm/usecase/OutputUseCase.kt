package de.jepfa.yapm.usecase

import de.jepfa.yapm.ui.BaseActivity

abstract class OutputUseCase<OUTPUT, ACTIVITY: BaseActivity> : UseCase<Unit, OUTPUT, ACTIVITY> {

    final override suspend fun execute(input: Unit, activity: ACTIVITY): UseCaseOutput<OUTPUT> {
        return execute(activity)
    }

    abstract fun execute(activity: ACTIVITY): UseCaseOutput<OUTPUT>
}