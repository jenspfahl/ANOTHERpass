package de.jepfa.yapm.ui

import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput

class UseCaseBackgroundLauncher<INPUT, OUTPUT, ACTIVITY: BaseActivity> (
    private val useCase: UseCase<INPUT, OUTPUT, ACTIVITY>
)
{
    
    fun launch(
        activity: ACTIVITY,
        input: INPUT) {
        launch(activity, input, {})
    }

    fun launch(
        activity: ACTIVITY,
        input: INPUT,
        postHandler: (backgroundResult: UseCaseOutput<OUTPUT>) -> Unit) {
        lateinit var result: UseCaseOutput<OUTPUT>
        AsyncWithProgressBar(activity,
            {
                result = useCase.execute(input, activity)
                result.success
            },
            {
                postHandler.invoke(result)
            })
    }

}