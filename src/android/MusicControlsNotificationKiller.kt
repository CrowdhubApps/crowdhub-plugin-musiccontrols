package com.homerours.musiccontrols

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class MusicControlsNotificationKiller : Service() {
    private var mNM: NotificationManager? = null
    private val mBinder: IBinder = KillBinder(this)
    override fun onBind(intent: Intent): IBinder {
        NOTIFICATION_ID = intent.getIntExtra("notificationID", 1)
        return mBinder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        mNM = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNM!!.cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        mNM = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNM!!.cancel(NOTIFICATION_ID)
    }

    companion object {
        private var NOTIFICATION_ID = 0
    }
}