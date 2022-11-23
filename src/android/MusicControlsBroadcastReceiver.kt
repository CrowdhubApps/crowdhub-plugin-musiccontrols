package com.homerours.musiccontrols

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.view.KeyEvent
import org.apache.cordova.CallbackContext

class MusicControlsBroadcastReceiver(private val musicControls: MusicControls) :
    BroadcastReceiver() {
    private var cb: CallbackContext? = null
    fun setCallback(cb: CallbackContext?) {
        this.cb = cb
    }

    fun stopListening() {
        if (cb != null) {
            cb!!.success("{\"message\": \"music-controls-stop-listening\" }")
            cb = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (cb != null) {
            val message = intent.action
            if (message == Intent.ACTION_HEADSET_PLUG) {
                // Headphone plug/unplug
                when (intent.getIntExtra("state", -1)) {
                    0 -> {
                        cb!!.success("{\"message\": \"music-controls-headset-unplugged\"}")
                        cb = null
                        musicControls.unregisterMediaButtonEvent()
                    }
                    1 -> {
                        cb!!.success("{\"message\": \"music-controls-headset-plugged\"}")
                        cb = null
                        musicControls.registerMediaButtonEvent()
                    }
                    else -> {}
                }
            } else if (message == "music-controls-media-button") {
                // Media button
                val event =
                    intent.getParcelableExtra<Parcelable>(Intent.EXTRA_KEY_EVENT) as KeyEvent?
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = event.keyCode
                    when (keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT -> cb!!.success("{\"message\": \"music-controls-media-button-next\"}")
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> cb!!.success("{\"message\": \"music-controls-media-button-pause\"}")
                        KeyEvent.KEYCODE_MEDIA_PLAY -> cb!!.success("{\"message\": \"music-controls-media-button-play\"}")
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> cb!!.success("{\"message\": \"music-controls-media-button-play-pause\"}")
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> cb!!.success("{\"message\": \"music-controls-media-button-previous\"}")
                        KeyEvent.KEYCODE_MEDIA_STOP -> cb!!.success("{\"message\": \"music-controls-media-button-stop\"}")
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> cb!!.success("{\"message\": \"music-controls-media-button-fast-forward\"}")
                        KeyEvent.KEYCODE_MEDIA_REWIND -> cb!!.success("{\"message\": \"music-controls-media-button-rewind\"}")
                        KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> cb!!.success("{\"message\": \"music-controls-media-button-skip-backward\"}")
                        KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> cb!!.success("{\"message\": \"music-controls-media-button-skip-forward\"}")
                        KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD -> cb!!.success("{\"message\": \"music-controls-media-button-step-backward\"}")
                        KeyEvent.KEYCODE_MEDIA_STEP_FORWARD -> cb!!.success("{\"message\": \"music-controls-media-button-step-forward\"}")
                        KeyEvent.KEYCODE_META_LEFT -> cb!!.success("{\"message\": \"music-controls-media-button-meta-left\"}")
                        KeyEvent.KEYCODE_META_RIGHT -> cb!!.success("{\"message\": \"music-controls-media-button-meta-right\"}")
                        KeyEvent.KEYCODE_MUSIC -> cb!!.success("{\"message\": \"music-controls-media-button-music\"}")
                        KeyEvent.KEYCODE_VOLUME_UP -> cb!!.success("{\"message\": \"music-controls-media-button-volume-up\"}")
                        KeyEvent.KEYCODE_VOLUME_DOWN -> cb!!.success("{\"message\": \"music-controls-media-button-volume-down\"}")
                        KeyEvent.KEYCODE_VOLUME_MUTE -> cb!!.success("{\"message\": \"music-controls-media-button-volume-mute\"}")
                        KeyEvent.KEYCODE_HEADSETHOOK -> cb!!.success("{\"message\": \"music-controls-media-button-headset-hook\"}")
                        else -> cb!!.success("{\"message\": \"$message\"}")
                    }
                    cb = null
                }
            } else if (message == "music-controls-destroy") {
                // Close Button
                cb!!.success("{\"message\": \"music-controls-destroy\"}")
                cb = null
                musicControls.destroyPlayerNotification()
            } else {
                cb!!.success("{\"message\": \"$message\"}")
                cb = null
            }
        }
    }
}