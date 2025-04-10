package com.example.drone

import android.content.Context
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class VlcPackageManager(context: Context) {

    private val libVLC: LibVLC
    private val mediaPlayer: MediaPlayer

    init {
        val options = ArrayList<String>().apply {
            add("--no-drop-late-frames")
            add("--no-skip-frames")
            add("--rtsp-tcp")
        }

        libVLC = LibVLC(context, options)
        mediaPlayer = MediaPlayer(libVLC)
    }

    fun getMediaPlayer(): MediaPlayer = mediaPlayer

    fun playStream(rtspUrl: String, outputPath: String? = null) {
        val media = Media(libVLC, rtspUrl)
        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=150")

        if (outputPath != null) {
            
            media.addOption(":sout=#file{dst=$outputPath}")
            media.addOption(":sout-keep")
        }

        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }

    fun stopAndRelease() {
        mediaPlayer.stop()
        mediaPlayer.detachViews()
        mediaPlayer.release()
        libVLC.release()
    }
}
