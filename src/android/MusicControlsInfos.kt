package com.homerours.musiccontrols

import org.json.JSONArray

class MusicControlsInfos(args: JSONArray) {
    var artist: String
    var album: String
    var track: String
    var ticker: String
    var cover: String
    var isPlaying: Boolean
    var hasPrev: Boolean
    var hasNext: Boolean
    var hasClose: Boolean
    var dismissable: Boolean
    var playIcon: String
    var pauseIcon: String
    var prevIcon: String
    var nextIcon: String
    var closeIcon: String
    var notificationIcon: String

    init {
        val params = args.getJSONObject(0)
        track = params.getString("track")
        artist = params.getString("artist")
        album = params.getString("album")
        ticker = params.getString("ticker")
        cover = params.getString("cover")
        isPlaying = params.getBoolean("isPlaying")
        hasPrev = params.getBoolean("hasPrev")
        hasNext = params.getBoolean("hasNext")
        hasClose = params.getBoolean("hasClose")
        dismissable = params.getBoolean("dismissable")
        playIcon = params.getString("playIcon")
        pauseIcon = params.getString("pauseIcon")
        prevIcon = params.getString("prevIcon")
        nextIcon = params.getString("nextIcon")
        closeIcon = params.getString("closeIcon")
        notificationIcon = params.getString("notificationIcon")
    }
}