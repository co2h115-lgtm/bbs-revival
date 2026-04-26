package com.bbsrevival.data

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.bbsrevival.MainActivity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class NotificationHelper @Inject constructor(private val context: Context) {

    private val nm = context.getSystemService(NotificationManager::class.java)

    private fun launchIntent() = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun notifyNewPM(fromHandle: String) {
        if (!nm.areNotificationsEnabled()) return
        nm.notify(
            Random.nextInt(),
            NotificationCompat.Builder(context, "bbs_pm")
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("New Private Message")
                .setContentText("$fromHandle sent you a message")
                .setAutoCancel(true)
                .setContentIntent(launchIntent())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    fun notifyMention(fromHandle: String, roomName: String) {
        if (!nm.areNotificationsEnabled()) return
        nm.notify(
            Random.nextInt(),
            NotificationCompat.Builder(context, "bbs_chat")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Mentioned in #$roomName")
                .setContentText("$fromHandle mentioned you")
                .setAutoCancel(true)
                .setContentIntent(launchIntent())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        )
    }

    fun notifyBroadcast(message: String) {
        if (!nm.areNotificationsEnabled()) return
        nm.notify(
            Random.nextInt(),
            NotificationCompat.Builder(context, "bbs_system")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("BBS Broadcast")
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(launchIntent())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )
    }
}
