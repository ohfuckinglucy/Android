package com.example.visprog

import android.content.ContentUris
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import android.util.Log

class PlayerViewModel(private val context: Context) : ViewModel() {

    private val _tracks = mutableStateListOf<AudioFile>()
    val tracks: List<AudioFile> get() = _tracks

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    var trackIndex by mutableStateOf(0)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var progress by mutableStateOf(0f)
        private set

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    progress = it.currentPosition.toFloat() / it.duration
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    fun loadAudioFiles() {
        _tracks.clear()
        _tracks.addAll(getAudioFiles(context))
    }

    private fun getAudioFiles(context: Context): List<AudioFile> {
        val audioList = mutableListOf<AudioFile>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                audioList.add(AudioFile(name, contentUri))
            }
        }
        return audioList
    }

    fun playTrack(index: Int) {
        if (_tracks.isEmpty()) return

        mediaPlayer?.release()
        trackIndex = index

        val trackUri = _tracks[trackIndex].uri
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, trackUri)
            prepare()
            start()
        }

        mediaPlayer?.setOnCompletionListener { nextTrack() }
        isPlaying = true
        progress = 0f
        handler.post(updateProgressRunnable)
    }

    fun nextTrack() {
        if (_tracks.isNotEmpty()) {
            playTrack((trackIndex + 1) % _tracks.size)
        }
    }

    fun previousTrack() {
        if (_tracks.isNotEmpty()) {
            playTrack(if (trackIndex > 0) trackIndex - 1 else _tracks.size - 1)
        }
    }

    fun playPause() {
        if (mediaPlayer == null && _tracks.isNotEmpty()) {
            playTrack(trackIndex)
        } else {
            if (isPlaying) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
                handler.post(updateProgressRunnable)
            }
            isPlaying = !isPlaying
        }
    }

    fun seekTo(progress: Float) {
        mediaPlayer?.let {
            val newPosition = (progress * it.duration).toInt()
            it.seekTo(newPosition)
        }
        this.progress = progress
    }

    override fun onCleared() {
        mediaPlayer?.release()
        handler.removeCallbacks(updateProgressRunnable)
        super.onCleared()
    }
}

data class AudioFile(
    val displayName: String,
    val uri: Uri
)
