package de.jepfa.yapm.service.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.autofill.ResponseFiller
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.util.removeTime
import java.util.*

class AlarmNotificationReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getIntExtra("ID", 0)
        Log.d("NOTIF", "scheduled notification with id=$id alarm received")

        PreferenceService.initStorage(context)

        val today = Date().removeTime()
        val expiresAtValues = PreferenceService.getAllStartingWith(PreferenceService.DATA_EXPIRY_DATES, context)
        val expiryDateForId =
            expiresAtValues.filterKeys { it == PreferenceService.DATA_EXPIRY_DATES + "_" + id }
                .map { it.key }
                .mapNotNull { PreferenceService.getAsString(it, context) }
                .mapNotNull { it.toLongOrNull() }
                .map { Date(it) }
                .map { it.removeTime() }
                .firstOrNull() //TODO { it == today }

        Log.d("NOTIF", "scheduled notification with id=$id alarm received having expiryDate=$expiryDateForId")

        if (expiryDateForId != null) {
            val contentIntent = createPendingExpiryIntent(context, id)
            NotificationService.pushNotification(
                context,
                NotificationService.CHANNEL_ID_SCHEDULED,
                "Cred Id expired " + id,
                "Expires at " + expiryDateForId,
                id,
                contentIntent
            )
        }
    }

    private fun createPendingExpiryIntent(context: Context, credentialId: Int): PendingIntent {
        val authIntent = Intent(context, ListCredentialsActivity::class.java)
        authIntent.putExtra(SecureActivity.SecretChecker.fromAutofill, true)

        authIntent.action = "${ResponseFiller.ACTION_OPEN_VAULT}}${ResponseFiller.ACTION_DELIMITER}$!!expired" // do it as extra doesn't work (extra gets lost)

        return PendingIntent.getActivity(
            context,
            credentialId,
            authIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ONE_SHOT
        )
    }
}