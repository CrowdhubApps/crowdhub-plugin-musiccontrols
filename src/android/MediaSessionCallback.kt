package com.homerours.musiccontrols

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import org.apache.cordova.CallbackContext

class MediaSessionCallback : MediaSessionCompat.Callback() {
    private var cb: CallbackContext? = null
    fun setCallback(cb: CallbackContext?) {
        this.cb = cb
    }

    override fun onPlay() {
        super.onPlay()
        if (cb != null) {
            cb!!.success("{\"message\": \"music-controls-media-button-play\"}")
            cb = null
        }
    }

    override fun onPause() {
        super.onPause()
        if (cb != null) {
            cb!!.success("{\"message\": \"music-controls-media-button-pause\"}")
            cb = null
        }
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        if (cb != null) {
            cb!!.success("{\"message\": \"music-controls-media-button-next\"}")
            cb = null
        }
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        if (cb != null) {
            cb!!.success("{\"message\": \"music-controls-media-button-previous\"}")
            cb = null
        }
    }

    override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
        super.onPlayFromMediaId(mediaId, extras)
    }

    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
        val event = mediaButtonIntent.extras!![Intent.EXTRA_KEY_EVENT] as KeyEvent?
            ?: return super.onMediaButtonEvent(mediaButtonIntent)
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_PAUSE -> if (cb != null) {
                    cb!!.success("{\"message\": \"music-controls-media-button-pause\"}")
                    cb = null
                }
                KeyEvent.KEYCODE_MEDIA_PLAY -> if (cb != null) {
                    cb!!.success("{\"message\": \"music-controls-media-button-play\"}")
                    cb = null
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> if (cb != null) {
                    cb!!.success("{\"message\": \"music-controls-media-button-previous\"}")
                    cb = null
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> if (cb != null) {
                    cb!!.success("{\"message\": \"music-controls-media-button-next\"}")
                    cb = null
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> if (cb != null) {
                    cb!!.success("{\"message\": \"music-controls-media-button-play-pause\"}")
                    cb = null
                }
                KeyEvent.KEYCODE_MEDIA_STOP -> if (cb != null) {
                    cb!!.success("{\"message\": \"music-controls-media-button-stop\"}")
                    cb = null
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> if (cb != null) {
                    cb!!.success("{\"message\": \"music-controls-media-button-forward\"}")
                    cb = null
                }
                KeyEvent.KEYCODE_MEDIA_REWIND -> if (cb != null) {
                    cb!!.success("{\"message\": \"music-controls-media-button-rewind\"}")
                    cb = null
                }
                else -> {
                    if (cb != null) {
                        cb!!.success("{\"message\": \"music-controls-media-button-unknown-$keyCode\"}")
                        cb = null
                    }
                    return super.onMediaButtonEvent(mediaButtonIntent)
                }
            }
        }
        return true
    }
}