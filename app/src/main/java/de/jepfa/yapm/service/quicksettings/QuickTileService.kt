package de.jepfa.yapm.service.quicksettings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.TileService
import de.jepfa.yapm.BuildConfig.APPLICATION_ID


class QuickTileService: TileService() {

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val launchIntent = packageManager.getLaunchIntentForPackage(APPLICATION_ID)
        if (launchIntent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    1001,
                    launchIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            }
            else {
                startActivityAndCollapse(launchIntent)
            }
        }
    }
}
