package de.jepfa.yapm.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.jepfa.yapm.R

object NotificationHelper {

    val CHANNEL_ID_PASTE = "de.jepfa.yapm.notificationchannel.paste"
    val NOTIFICATION_ID_PASTE_SUCCESS = 1001

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

    fun pushNotification(context: Context, channelId: String, title: String, text:String, codeSuccess: Int) {
        val mBuilder =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_baseline_vpn_key_24_white)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(
            codeSuccess,
            mBuilder.build()
        )

    }
}
