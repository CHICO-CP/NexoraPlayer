package com.nexora.player

import android.net.Uri
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.nexora.player.databinding.ActivityPlayerBinding
import com.nexora.player.utils.TimeUtils

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URI      = "extra_uri"
        const val EXTRA_TITLE    = "extra_title"
        const val EXTRA_ARTIST   = "extra_artist"
        const val EXTRA_IS_VIDEO = "extra_is_video"
    }

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var isPlaying = false

    private val progressUpdater = object : Runnable {
        override fun run() {
            updateProgress()
            binding.seekbarPlayer.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uriStr  = intent.getStringExtra(EXTRA_URI)
        val title   = intent.getStringExtra(EXTRA_TITLE)   ?: "Unknown"
        val artist  = intent.getStringExtra(EXTRA_ARTIST)  ?: "Unknown Artist"

        binding.tvSongTitle.text  = title
        binding.tvArtistName.text = artist

        binding.btnBackPlayer.setOnClickListener { finish() }
        binding.btnFavorite.setOnClickListener {
            val drawable = if (binding.btnFavorite.tag == "fav") {
                binding.btnFavorite.tag = null
                R.drawable.ic_favorite_border
            } else {
                binding.btnFavorite.tag = "fav"
                R.drawable.ic_favorite
            }
            binding.btnFavorite.setImageResource(drawable)
        }

        if (uriStr != null) initPlayer(Uri.parse(uriStr))
        setupSeekbar()
    }

    private fun initPlayer(uri: Uri) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            exo.setMediaItem(ExoMediaItem.fromUri(uri))
            exo.prepare()
            exo.play()
            isPlaying = true
            updatePlayPauseIcon()
        }
        binding.btnPlayPause.setOnClickListener { togglePlay() }
        binding.btnNext.setOnClickListener { player?.seekTo((player!!.currentPosition + 15_000).coerceAtMost(player!!.duration)) }
        binding.btnPrev.setOnClickListener { player?.seekTo((player!!.currentPosition - 15_000).coerceAtLeast(0)) }
        binding.seekbarPlayer.postDelayed(progressUpdater, 500)
    }

    private fun setupSeekbar() {
        binding.seekbarPlayer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) player?.seekTo(progress.toLong())
            }
            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {}
        })
    }

    private fun togglePlay() {
        player?.let { p ->
            if (p.isPlaying) { p.pause(); isPlaying = false } else { p.play(); isPlaying = true }
            updatePlayPauseIcon()
        }
    }

    private fun updateProgress() {
        player?.let { p ->
            val pos = p.currentPosition
            val dur = p.duration.coerceAtLeast(0)
            binding.seekbarPlayer.max      = dur.toInt()
            binding.seekbarPlayer.progress = pos.toInt()
            binding.tvCurrentTime.text     = TimeUtils.formatDuration(pos)
            binding.tvTotalTime.text       = TimeUtils.formatDuration(dur)
        }
    }

    private fun updatePlayPauseIcon() {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    override fun onDestroy() {
        binding.seekbarPlayer.removeCallbacks(progressUpdater)
        player?.release(); player = null
        super.onDestroy()
    }
}
