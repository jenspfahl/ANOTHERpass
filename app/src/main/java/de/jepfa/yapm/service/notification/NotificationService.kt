package de.jepfa.yapm.service.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.jepfa.yapm.R
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.PermissionChecker.hasNotificationPermission
import java.util.*

object NotificationService {

    val CHANNEL_ID_PASTE = "de.jepfa.yapm.notificationchannel.paste"
    val CHANNEL_ID_SCHEDULED = "de.jepfa.yapm.notificationchannel.scheduled"

    val NOTIFICATION_ID_PASTE_SUCCESS = 1001
    val SCHEDULED_NOTIFICATION_SUCCESS = 1002

    val SCHEDULED_NOTIFICATION_KEY_SEPARATOR = "#"


    fun createNotificationChannel(context: Context, channelId: String, name: String) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission") // permission check in hasNotificationPermission()
    fun pushNotification(context: Context, channelId: String, title: String, text:String, notificationId: Int, silent: Boolean,
                         contentIntent: PendingIntent? = null, actionTitle: String? = null, actionIntent: PendingIntent? = null) {
        val mBuilder =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_keywithqrcode_normal)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setSilent(silent)
                .setColor(context.getColor(R.color.colorAccent))

        if (actionTitle != null && actionIntent != null) {
            mBuilder
                .addAction(0, actionTitle, actionIntent)
        }

        if (!hasNotificationPermission(context)) {
            Log.w(LOG_PREFIX + "NOTIF", "notification permission not granted")
            return
        }
        else {
            val notificationManager = NotificationManagerCompat.from(context)

            // permission check done above
            notificationManager.notify(
                notificationId,
                mBuilder.build()
            )
        }

    }

    fun scheduleNotification(context: Context, id: Int, date: Date) {

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = createAlarmPendingIntent(context, id)

        val calendar = Calendar.getInstance()
        calendar.time = date

        // for testing:
        //calendar.timeInMillis = System.currentTimeMillis()
        //calendar.add(Calendar.MINUTE, 1)


        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            alarmIntent
        )

        Log.d(LOG_PREFIX + "NOTIF", "scheduling notification with id=$id and date=$date")
    }

    fun cancelScheduledNotification(context: Context, id: Int) {

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = createAlarmPendingIntent(context, id)

        alarmManager.cancel(alarmIntent)

        Log.d(LOG_PREFIX + "NOTIF", "cancelling notification with id=$id")

    }

    private fun createAlarmPendingIntent(
        context: Context,
        id: Int
    ): PendingIntent? {
        val alarmIntent = Intent(context, ExpiryAlarmNotificationReceiver::class.java).let { intent ->
            intent.putExtra("ID", id)
            PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return alarmIntent
    }

}
