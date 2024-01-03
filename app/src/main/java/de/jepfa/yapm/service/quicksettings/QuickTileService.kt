package de.jepfa.yapm.service.quicksettings

import android.service.quicksettings.TileService
import de.jepfa.yapm.BuildConfig.APPLICATION_ID


class QuickTileService: TileService() {

    override fun onClick() {
        val launchIntent = packageManager.getLaunchIntentForPackage(APPLICATION_ID)
        startActivityAndCollapse(launchIntent)
    }
}
