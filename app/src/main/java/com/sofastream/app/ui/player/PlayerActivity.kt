package com.sofastream.app.ui.player

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.View
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.TimeBar
import com.sofastream.app.R
import com.sofastream.app.SofaStreamApp
import com.sofastream.app.api.ApiClient
import com.sofastream.app.data.model.PlaybackInfo
import com.sofastream.app.data.repository.JellyfinRepository
import com.sofastream.app.databinding.ActivityPlayerBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYBACK_INFO = "extra_playback_info"
        private const val CONTROLS_HIDE_DELAY_MS = 4000L
        private const val SKIP_DURATION_MS = 10_000L
        private const val PROGRESS_REPORT_INTERVAL_MS = 10_000L
    }

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var playbackInfo: PlaybackInfo? = null

    private val hideControlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }

    private var progressReportJob: Job? = null
    private var timeBarUpdateJob: Job? = null
    private var controlsVisible = true

    private val prefs by lazy { SofaStreamApp.instance.userPreferences }

    private fun getJellyfinRepo(): JellyfinRepository {
        return JellyfinRepository(
            api = ApiClient.getJellyfinApi(prefs.getJellyfinUrlSync()),
            baseUrl = prefs.getJellyfinUrlSync(),
            token = prefs.getJellyfinTokenSync(),
            userId = prefs.getJellyfinUserIdSync()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // For Android R+ use WindowInsetsController; fall back to deprecated systemUiVisibility for older APIs
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }

        playbackInfo = intent.getParcelableExtra(EXTRA_PLAYBACK_INFO)
        if (playbackInfo == null) {
            finish()
            return
        }

        setupPlayer()
        setupControls()
        setupMetadata()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()

        binding.playerView.player = player

        val info = playbackInfo ?: return
        val mediaItem = MediaItem.fromUri(info.streamUrl)
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            if (info.startPositionTicks > 0) {
                seekTo(info.startPositionTicks / 10_000)
            }
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    updatePlayPauseButton()
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            binding.loadingIndicator.isVisible = true
                        }
                        Player.STATE_READY -> {
                            binding.loadingIndicator.isVisible = false
                            startProgressReporting()
                            startTimeBarUpdates()
                            reportPlaybackStart()
                        }
                        Player.STATE_ENDED -> {
                            binding.loadingIndicator.isVisible = false
                            reportPlaybackStop()
                        }
                        else -> {
                            binding.loadingIndicator.isVisible = false
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButton()
                    if (isPlaying) {
                        scheduleHideControls()
                    } else {
                        hideControlsHandler.removeCallbacks(hideControlsRunnable)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    binding.loadingIndicator.isVisible = false
                    binding.tvError.isVisible = true
                    binding.tvError.text = getString(R.string.player_error, error.message)
                }
            })
        }

        binding.playerView.useController = false
        binding.playerView.setOnClickListener { toggleControls() }
        setupTimeBar()
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            player?.let {
                if (it.isPlaying) it.pause() else it.play()
            }
            scheduleHideControls()
        }

        binding.btnRewind.setOnClickListener {
            player?.let { p ->
                val newPos = (p.currentPosition - SKIP_DURATION_MS).coerceAtLeast(0)
                p.seekTo(newPos)
                showSkipIndicator(false)
            }
            scheduleHideControls()
        }

        binding.btnForward.setOnClickListener {
            player?.let { p ->
                val newPos = (p.currentPosition + SKIP_DURATION_MS).coerceAtMost(p.duration.coerceAtLeast(0))
                p.seekTo(newPos)
                showSkipIndicator(true)
            }
            scheduleHideControls()
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnPip.setOnClickListener {
            enterPipMode()
        }

        binding.btnPip.isVisible = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

        showControls()
    }

    private fun setupMetadata() {
        val info = playbackInfo ?: return
        val titleText = when {
            info.seriesName != null -> info.seriesName
            else -> info.title
        }
        val subtitleText = when {
            info.seriesName != null -> buildString {
                info.seasonNumber?.let { append("S$it") }
                info.episodeNumber?.let { append("E$it") }
                if (isNotEmpty()) append(" • ")
                append(info.title)
            }
            else -> null
        }

        binding.tvPlayerTitle.text = titleText
        binding.tvPlayerSubtitle.text = subtitleText
        binding.tvPlayerSubtitle.isVisible = !subtitleText.isNullOrEmpty()
    }

    private fun toggleControls() {
        if (controlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls() {
        controlsVisible = true
        binding.playerControlsContainer.isVisible = true
        binding.playerTopBar.isVisible = true
        scheduleHideControls()
    }

    private fun hideControls() {
        controlsVisible = false
        binding.playerControlsContainer.isVisible = false
        binding.playerTopBar.isVisible = false
    }

    private fun scheduleHideControls() {
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
        if (player?.isPlaying == true) {
            hideControlsHandler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_DELAY_MS)
        }
    }

    private fun updatePlayPauseButton() {
        val isPlaying = player?.isPlaying ?: false
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun showSkipIndicator(isForward: Boolean) {
        val indicator = if (isForward) binding.skipForwardIndicator else binding.skipBackwardIndicator
        indicator.isVisible = true
        Handler(Looper.getMainLooper()).postDelayed({ indicator.isVisible = false }, 800)
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            binding.playerControlsContainer.isVisible = false
            binding.playerTopBar.isVisible = false
            binding.playerProgressBar.isVisible = false
            hideControlsHandler.removeCallbacks(hideControlsRunnable)
        } else {
            binding.playerProgressBar.isVisible = true
            showControls()
        }
    }

    private fun setupTimeBar() {
        binding.playerProgressBar.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {}
            override fun onScrubMove(timeBar: TimeBar, position: Long) {}
            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                if (!canceled) {
                    player?.seekTo(position)
                }
                scheduleHideControls()
            }
        })
    }

    private fun startTimeBarUpdates() {
        timeBarUpdateJob?.cancel()
        timeBarUpdateJob = lifecycleScope.launch {
            while (isActive) {
                val p = player ?: break
                val duration = p.duration.takeIf { it > 0 } ?: 0
                val position = p.currentPosition
                val buffered = p.bufferedPosition
                binding.playerProgressBar.setDuration(duration)
                binding.playerProgressBar.setPosition(position)
                binding.playerProgressBar.setBufferedPosition(buffered)
                delay(500)
            }
        }
    }

    private fun startProgressReporting() {
        progressReportJob?.cancel()
        progressReportJob = lifecycleScope.launch {
            while (isActive) {
                delay(PROGRESS_REPORT_INTERVAL_MS)
                val info = playbackInfo ?: continue
                val positionTicks = (player?.currentPosition ?: 0) * 10_000
                val isPaused = player?.isPlaying == false
                getJellyfinRepo().reportPlaybackProgress(
                    info.mediaItemId, info.mediaSourceId, info.playSessionId,
                    positionTicks, isPaused
                )
            }
        }
    }

    private fun reportPlaybackStart() {
        val info = playbackInfo ?: return
        lifecycleScope.launch {
            getJellyfinRepo().reportPlaybackStart(info.mediaItemId, info.mediaSourceId, info.playSessionId)
        }
    }

    private fun reportPlaybackStop() {
        val info = playbackInfo ?: return
        val positionTicks = (player?.currentPosition ?: 0) * 10_000
        lifecycleScope.launch {
            getJellyfinRepo().reportPlaybackStop(info.mediaItemId, info.mediaSourceId, info.playSessionId, positionTicks)
        }
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onPause() {
        super.onPause()
        if (!isInPictureInPictureMode) {
            player?.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isInPictureInPictureMode) {
            reportPlaybackStop()
            progressReportJob?.cancel()
            timeBarUpdateJob?.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
        progressReportJob?.cancel()
        timeBarUpdateJob?.cancel()
        player?.release()
        player = null
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            enterPipMode()
        }
    }
}
