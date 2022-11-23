package com.homerours.musiccontrols

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.homerours.musiccontrols.MusicControlsNotificationKiller
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaInterface
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CordovaWebView
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class MusicControls : CordovaPlugin() {
    private var mMessageReceiver: MusicControlsBroadcastReceiver? = null
    private var notification: MusicControlsNotification? = null
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var mediaButtonPendingIntent: PendingIntent? = null
    private var cordovaActivity: Activity? = null
    private val mMediaSessionCallback = MediaSessionCallback()
    private fun registerBroadcaster(mMessageReceiver: MusicControlsBroadcastReceiver) {
        val context = cordova.activity.applicationContext
        context.registerReceiver(
            mMessageReceiver as BroadcastReceiver,
            IntentFilter("music-controls-previous")
        )
        context.registerReceiver(
            mMessageReceiver as BroadcastReceiver,
            IntentFilter("music-controls-pause")
        )
        context.registerReceiver(
            mMessageReceiver as BroadcastReceiver,
            IntentFilter("music-controls-play")
        )
        context.registerReceiver(
            mMessageReceiver as BroadcastReceiver,
            IntentFilter("music-controls-next")
        )
        context.registerReceiver(
            mMessageReceiver as BroadcastReceiver,
            IntentFilter("music-controls-media-button")
        )
        context.registerReceiver(
            mMessageReceiver as BroadcastReceiver,
            IntentFilter("music-controls-destroy")
        )

        // Listen for headset plug/unplug
        context.registerReceiver(
            mMessageReceiver as BroadcastReceiver,
            IntentFilter(Intent.ACTION_HEADSET_PLUG)
        )
    }

    // Register pendingIntent for broacast
    fun registerMediaButtonEvent() {
        mediaSessionCompat!!.setMediaButtonReceiver(mediaButtonPendingIntent)

        /*if (this.mediaButtonAccess && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
		this.mAudioManager.registerMediaButtonEventReceiver(this.mediaButtonPendingIntent);
		}*/
    }

    fun unregisterMediaButtonEvent() {
        mediaSessionCompat!!.setMediaButtonReceiver(null)
        /*if (this.mediaButtonAccess && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2){
		this.mAudioManager.unregisterMediaButtonEventReceiver(this.mediaButtonPendingIntent);
		}*/
    }

    fun destroyPlayerNotification() {
        notification!!.destroy()
    }

    override fun initialize(cordova: CordovaInterface, webView: CordovaWebView) {
        super.initialize(cordova, webView)
        val activity: Activity = this.cordova.activity
        val context = activity.applicationContext
        cordovaActivity = activity
        val notificationID = 7824
        notification = MusicControlsNotification(activity, notificationID)
        mMessageReceiver = MusicControlsBroadcastReceiver(this)
        registerBroadcaster(mMessageReceiver!!)
        mediaSessionCompat = MediaSessionCompat(
            context,
            "cordova-music-controls-media-session",
            null,
            mediaButtonPendingIntent
        )
        mediaSessionCompat!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        mediaSessionCompat!!.isActive = true
        mediaSessionCompat!!.setCallback(mMediaSessionCallback)

        // Register media (headset) button event receiver
        try {
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            }
            val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val headsetIntent = Intent("music-controls-media-button")
            mediaButtonPendingIntent = PendingIntent.getBroadcast(context, 0, headsetIntent, flags)
            registerMediaButtonEvent()
        } catch (e: Exception) {
            val mediaButtonAccess = false
            e.printStackTrace()
        }

        // Notification Killer
        val mConnection: ServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, binder: IBinder) {
                (binder as KillBinder).service.startService(
                    Intent(
                        activity,
                        MusicControlsNotificationKiller::class.java
                    )
                )
            }

            override fun onServiceDisconnected(className: ComponentName) {}
        }
        val startServiceIntent = Intent(activity, MusicControlsNotificationKiller::class.java)
        startServiceIntent.putExtra("notificationID", notificationID)
        activity.bindService(startServiceIntent, mConnection, Context.BIND_AUTO_CREATE)
    }

    @Throws(JSONException::class)
    override fun execute(
        action: String,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        val context = cordova.activity.applicationContext
        val activity: Activity = cordova.activity
        if (action == "create") {
            val infos = MusicControlsInfos(args)
            val metadataBuilder = MediaMetadataCompat.Builder()
            cordova.threadPool.execute {
                notification!!.updateNotification(infos)

                // track title
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, infos.track)
                // artists
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, infos.artist)
                //album
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, infos.album)
                val art = getBitmapCover(infos.cover)
                if (art != null) {
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art)
                }
                mediaSessionCompat!!.setMetadata(metadataBuilder.build())
                if (infos.isPlaying) setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING) else setMediaPlaybackState(
                    PlaybackStateCompat.STATE_PAUSED
                )
                callbackContext.success("success")
            }
        } else if (action == "updateIsPlaying") {
            val params = args.getJSONObject(0)
            val isPlaying = params.getBoolean("isPlaying")
            notification!!.updateIsPlaying(isPlaying)
            if (isPlaying) setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING) else setMediaPlaybackState(
                PlaybackStateCompat.STATE_PAUSED
            )
            callbackContext.success("success")
        } else if (action == "updateDismissable") {
            val params = args.getJSONObject(0)
            val dismissable = params.getBoolean("dismissable")
            notification!!.updateDismissable(dismissable)
            callbackContext.success("success")
        } else if (action == "destroy") {
            notification!!.destroy()
            mMessageReceiver!!.stopListening()
            callbackContext.success("success")
        } else if (action == "watch") {
            registerMediaButtonEvent()
            cordova.threadPool.execute {
                mMediaSessionCallback.setCallback(callbackContext)
                mMessageReceiver!!.setCallback(callbackContext)
            }
        }
        return true
    }

    override fun onDestroy() {
        notification!!.destroy()
        mMessageReceiver!!.stopListening()
        unregisterMediaButtonEvent()
        super.onDestroy()
    }

    override fun onReset() {
        onDestroy()
        super.onReset()
    }

    private fun setMediaPlaybackState(state: Int) {
        val playbackstateBuilder = PlaybackStateCompat.Builder()
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            playbackstateBuilder.setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
            )
            playbackstateBuilder.setState(
                state,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1.0f
            )
        } else {
            playbackstateBuilder.setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
            )
            playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        }
        mediaSessionCompat!!.setPlaybackState(playbackstateBuilder.build())
    }

    // Get image from url
    private fun getBitmapCover(coverURL: String?): Bitmap? {
        return try {
            if (coverURL!!.matches("^(https?|ftp)://.*$".toRegex())) // Remote image
                getBitmapFromURL(coverURL) else {
                // Local image
                getBitmapFromLocal(coverURL)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
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
                val fileStream = cordovaActivity!!.assets.open("www/$localURL")
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
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (ex: Exception) {
            ex.printStackTrace()
            null
        }
    }
}
