package de.jepfa.yapm.service.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.util.removeTime
import de.jepfa.yapm.util.toDate
import java.util.*

class NotificationScheduleBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action ==Intent.ACTION_BOOT_COMPLETED) {
            PreferenceService.initStorage(context)

            val expiresAtValues = PreferenceService.getAllStartingWith(PreferenceService.DATA_EXPIRY_DATES, context)
            expiresAtValues
                .forEach {
                    val id = it.key.substringAfter("_").toIntOrNull()
                    val expiresAt = it.value.toString().toLongOrNull()?.toDate()
                    Log.d("NOTIF", "after boot: scheduled notification with id=$id rescheduling...")

                    if (id != null && expiresAt != null) {
                        NotificationService.scheduleNotification(context, id, expiresAt)
                    }

                }
        }
    }
}
