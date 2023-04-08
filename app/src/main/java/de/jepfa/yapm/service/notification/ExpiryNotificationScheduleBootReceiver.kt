package de.jepfa.yapm.service.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.notification.NotificationService.SCHEDULED_NOTIFICATION_KEY_SEPARATOR
import de.jepfa.yapm.util.toDate

class ExpiryNotificationScheduleBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NOTIF", "boot completed: intent=$intent")

        if (intent.action ==Intent.ACTION_BOOT_COMPLETED) {

            PreferenceService.initStorage(context)

            val expiresAtValues = PreferenceService.getAllStartingWith(PreferenceService.DATA_EXPIRY_DATES, context)
            Log.d("NOTIF", "expiresAtValues=$expiresAtValues")


            expiresAtValues
                .forEach {
                    val id = it.key.substringAfter(SCHEDULED_NOTIFICATION_KEY_SEPARATOR).toIntOrNull()
                    val expiresAt = it.value.toString().toLongOrNull()?.toDate()
                    Log.d("NOTIF", "after boot: scheduled notification with id=$id rescheduling...")

                    if (id != null && expiresAt != null) {
                        NotificationService.scheduleNotification(context, id, expiresAt)
                    }

                }
        }
    }
}
