package de.jepfa.yapm.usecase.webextension

import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.usecase.UseCase
import de.jepfa.yapm.usecase.UseCaseOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object DeleteDisabledWebExtensionsUseCase: UseCase<Unit, Int, BaseActivity> {

    override suspend fun execute(unit: Unit, activity: BaseActivity): UseCaseOutput<Int> {
        var disabledCount = 0

        CoroutineScope(Dispatchers.IO).launch {
            activity.getApp().webExtensionRepository.getAllSync().forEach { webExtension ->
                if (!webExtension.enabled) {
                    DeleteWebExtensionUseCase.execute(webExtension, activity)
                    disabledCount++;
                }
            }
        }.join()

        return UseCaseOutput(disabledCount)
    }

}