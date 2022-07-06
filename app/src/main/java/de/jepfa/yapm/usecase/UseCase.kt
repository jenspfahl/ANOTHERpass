package de.jepfa.yapm.usecase

import de.jepfa.yapm.ui.BaseActivity

interface UseCase<INPUT, OUTPUT, ACTIVITY: BaseActivity> {
    /**
     * Execute supposed to be run in a background job.
     */
    suspend fun execute(input: INPUT, activity: ACTIVITY): UseCaseOutput<OUTPUT>
}