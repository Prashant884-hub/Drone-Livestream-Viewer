package com.example.drone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.drone.databinding.ActivityMainBinding
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.LibVLC
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import android.view.View
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer
    private var isRecording: Boolean = false
    private var recordFile: File? = null
    private var vlcManager: VlcPackageManager? = null
    private var currentRecordingPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ask for storage permissions if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                101
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize VLC
        val options = arrayListOf("--no-drop-late-frames", "--no-skip-frames", "--rtsp-tcp")

        libVLC = LibVLC(this, options)
        mediaPlayer = MediaPlayer(libVLC)

        // Attach VLCVideoLayout
        mediaPlayer.attachViews(binding.videoSurface, null, false, false)

        binding.playButton.setOnClickListener {
            val url = binding.urlInput.text.toString()
            if (url.isNotBlank()) {
                playStream(url)
            } else {
                Toast.makeText(this, "Please enter a valid RTSP URL", Toast.LENGTH_SHORT).show()
            }
        }
        binding.pipButton.setOnClickListener {
            enterPipMode()
        }


        binding.recordButton.setOnClickListener {
            toggleRecording()
        }
    }

    fun playStream(rtspUrl: String, record: Boolean = false, outputPath: String? = null) {
        val uri = android.net.Uri.parse(rtspUrl)  // <-- Parse properly
        val media = Media(libVLC, uri)

        media.addOption(":network-caching=150")

        if (record && outputPath != null) {
            media.addOption(":sout=#file{dst=$outputPath}")
            media.addOption(":sout-keep")
        }

        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()
    }


    private fun toggleRecording() {
        if (!isRecording) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "RTSP_Record_$timeStamp.mp4"
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Recordings")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)

            // Stop if already playing
            mediaPlayer.stop()

            // Play again with recording enabled
            playStream(binding.urlInput.text.toString(), true, file.absolutePath)

            isRecording = true
            recordFile = file
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } else {
            mediaPlayer.stop()
            isRecording = false
            Toast.makeText(this, "Recording stopped: ${recordFile?.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(binding.videoSurface.width, binding.videoSurface.height)
            val pipBuilder = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            enterPictureInPictureMode(pipBuilder)
        } else {
            Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        // Hide everything except video when in PiP
        if (isInPictureInPictureMode) {
            binding.urlInput.visibility = View.GONE
            binding.playButton.visibility = View.GONE
            binding.recordButton.visibility = View.GONE
            binding.pipButton.visibility = View.GONE
        } else {
            binding.urlInput.visibility = View.VISIBLE
            binding.playButton.visibility = View.VISIBLE
            binding.recordButton.visibility = View.VISIBLE
            binding.pipButton.visibility = View.VISIBLE
        }

    }
        override fun onDestroy() {
            super.onDestroy()
            mediaPlayer.stop()
            mediaPlayer.detachViews()
            mediaPlayer.release()
            libVLC.release()
        }
    }
