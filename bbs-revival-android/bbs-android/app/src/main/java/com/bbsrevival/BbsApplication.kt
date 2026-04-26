package com.bbsrevival

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BbsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel("bbs_pm",    "Private Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "New private message notifications"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel("bbs_chat",  "Chat Mentions",    NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "When someone mentions you in chat"
                }
            )
            nm.createNotificationChannel(
                NotificationChannel("bbs_system","System Broadcasts",NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Sysop broadcast messages"
                }
            )
        }
    }
}
