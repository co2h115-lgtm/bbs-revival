package com.bbsrevival.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            listOf(
                NotificationChannel("bbs_pm",     "Private Messages", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel("bbs_chat",   "Chat Mentions",    NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel("bbs_system", "System Broadcasts",NotificationManager.IMPORTANCE_LOW),
            ).forEach { nm.createNotificationChannel(it) }
        }
    }
}
