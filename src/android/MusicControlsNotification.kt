package com.homerours.musiccontrols

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class MusicControlsNotification(
    private val cordovaActivity: Activity,
    private val notificationID: Int
) {
    private val notificationManager: NotificationManager
    private var notificationBuilder: Notification.Builder? = null
    private var infos: MusicControlsInfos? = null
    private var bitmapCover: Bitmap? = null
    private val CHANNEL_ID = "cordova-music-channel-id"

    // Public Constructor
    init {
        val context: Context = cordovaActivity
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // use channelid for Oreo and higher
        if (Build.VERSION.SDK_INT >= 26) {
            // The user-visible name of the channel.
            val name: CharSequence = "Audio Controls"
            // The user-visible description of the channel.
            val description = "Control Playing Audio"
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)

            // Configure the notification channel.
            mChannel.description = description
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    // Show or update notification
    fun updateNotification(newInfos: MusicControlsInfos) {
        // Check if the cover has changed	
        if (newInfos.cover.isNotEmpty() && (infos == null || newInfos.cover != infos!!.cover)) {
            getBitmapCover(newInfos.cover)
        }
        infos = newInfos
        createBuilder()
        val noti = notificationBuilder!!.build()
        notificationManager.notify(notificationID, noti)
    }

    // Toggle the play/pause button
    fun updateIsPlaying(isPlaying: Boolean) {
        infos!!.isPlaying = isPlaying
        createBuilder()
        val noti = notificationBuilder!!.build()
        notificationManager.notify(notificationID, noti)
    }

    // Toggle the dismissable status
    fun updateDismissable(dismissable: Boolean) {
        infos!!.dismissable = dismissable
        createBuilder()
        val noti = notificationBuilder!!.build()
        notificationManager.notify(notificationID, noti)
    }

    // Get image from url
    private fun getBitmapCover(coverURL: String?) {
        try {
            bitmapCover = if (coverURL!!.matches("^(https?|ftp)://.*$".toRegex())) // Remote image
                getBitmapFromURL(coverURL) else {
                // Local image
                getBitmapFromLocal(coverURL)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    // get Local image
    private fun getBitmapFromLocal(localURL: String?): Bitmap? {
        return try {
            val uri = Uri.parse(localURL)
            val file = uri.path?.let { File(it) }
            val fileStream = FileInputStream(file)
            val buf = BufferedInputStream(fileStream)
            val myBitmap = BitmapFactory.decodeStream(buf)
            buf.close()
            myBitmap
        } catch (ex: Exception) {
            try {
                val fileStream = cordovaActivity.assets.open("www/$localURL")
                val buf = BufferedInputStream(fileStream)
                val myBitmap = BitmapFactory.decodeStream(buf)
                buf.close()
                myBitmap
            } catch (ex2: Exception) {
                ex.printStackTrace()
                ex2.printStackTrace()
                null
            }
        }
    }

    // get Remote image
    private fun getBitmapFromURL(strURL: String?): Bitmap? {
        return try {
            val url = URL(strURL)
            val connection =
                url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }

    private fun createBuilder() {
        val context: Context = cordovaActivity
        val builder = Notification.Builder(context)

        // use channelid for Oreo and higher
        if (Build.VERSION.SDK_INT >= 26) {
            builder.setChannelId(CHANNEL_ID)
        }

        //Configure builder
        builder.setContentTitle(infos!!.track)
        if (infos!!.artist.isNotEmpty()) {
            builder.setContentText(infos!!.artist)
        }
        builder.setWhen(0)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        }
        // set if the notification can be destroyed by swiping
        if (infos!!.dismissable) {
            builder.setOngoing(false)
            val dismissIntent = Intent("music-controls-destroy")
            val dismissPendingIntent = PendingIntent.getBroadcast(context, 1, dismissIntent, flags)
            builder.setDeleteIntent(dismissPendingIntent)
        } else {
            builder.setOngoing(true)
        }
        if (infos!!.ticker.isNotEmpty()) {
            builder.setTicker(infos!!.ticker)
        }
        builder.setPriority(Notification.PRIORITY_MAX)

        //If 5.0 >= set the controls to be visible on lockscreen
        builder.setVisibility(Notification.VISIBILITY_PUBLIC)

        //Set SmallIcon
        var usePlayingIcon = infos!!.notificationIcon.isEmpty()
        if (!usePlayingIcon) {
            val resId = getResourceId(infos!!.notificationIcon, 0)
            usePlayingIcon = resId == 0
            if (!usePlayingIcon) {
                builder.setSmallIcon(resId)
            }
        }
        if (usePlayingIcon) {
            if (infos!!.isPlaying) {
                builder.setSmallIcon(getResourceId(infos!!.playIcon, android.R.drawable.ic_media_play))
            } else {
                builder.setSmallIcon(getResourceId(infos!!.pauseIcon, android.R.drawable.ic_media_pause))
            }
        }

        //Set LargeIcon
        if (infos!!.cover.isNotEmpty() && bitmapCover != null) {
            builder.setLargeIcon(bitmapCover)
        }

        //Open app if tapped
        val resultIntent = Intent(context, cordovaActivity.javaClass)
        resultIntent.action = Intent.ACTION_MAIN
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, flags)
        builder.setContentIntent(resultPendingIntent)

        //Controls
        var nbControls = 0
        /* Previous  */if (infos!!.hasPrev) {
            nbControls++
            val previousIntent = Intent("music-controls-previous")
            val previousPendingIntent =
                PendingIntent.getBroadcast(context, 1, previousIntent, flags)
            builder.addAction(
                getResourceId(infos!!.prevIcon, android.R.drawable.ic_media_previous),
                "",
                previousPendingIntent
            )
        }
        if (infos!!.isPlaying) {
            /* Pause  */
            nbControls++
            val pauseIntent = Intent("music-controls-pause")
            val pausePendingIntent = PendingIntent.getBroadcast(context, 1, pauseIntent, flags)
            builder.addAction(
                getResourceId(infos!!.pauseIcon, android.R.drawable.ic_media_pause),
                "",
                pausePendingIntent
            )
        } else {
            /* Play  */
            nbControls++
            val playIntent = Intent("music-controls-play")
            val playPendingIntent = PendingIntent.getBroadcast(context, 1, playIntent, flags)
            builder.addAction(
                getResourceId(infos!!.playIcon, android.R.drawable.ic_media_play),
                "",
                playPendingIntent
            )
        }
        /* Next */if (infos!!.hasNext) {
            nbControls++
            val nextIntent = Intent("music-controls-next")
            val nextPendingIntent = PendingIntent.getBroadcast(context, 1, nextIntent, flags)
            builder.addAction(
                getResourceId(infos!!.nextIcon, android.R.drawable.ic_media_next),
                "",
                nextPendingIntent
            )
        }
        /* Close */if (infos!!.hasClose) {
            nbControls++
            val destroyIntent = Intent("music-controls-destroy")
            val destroyPendingIntent = PendingIntent.getBroadcast(context, 1, destroyIntent, flags)
            builder.addAction(
                getResourceId(
                    infos!!.closeIcon,
                    android.R.drawable.ic_menu_close_clear_cancel
                ), "", destroyPendingIntent
            )
        }

        //If 5.0 >= use MediaStyle
        val args = IntArray(nbControls)
        for (i in 0 until nbControls) {
            args[i] = i
        }
        builder.style = Notification.MediaStyle().setShowActionsInCompactView(*args)
        notificationBuilder = builder
    }

    private fun getResourceId(name: String?, fallback: Int): Int {
        return try {
            if (name!!.isEmpty()) {
                return fallback
            }
            val resId = cordovaActivity.resources.getIdentifier(
                name,
                "drawable",
                cordovaActivity.packageName
            )
            if (resId == 0) fallback else resId
        } catch (ex: Exception) {
            fallback
        }
    }

    fun destroy() {
        notificationManager.cancel(notificationID)
    }
}