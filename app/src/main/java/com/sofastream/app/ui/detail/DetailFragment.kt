package com.sofastream.app.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.chip.Chip
import com.sofastream.app.R
import com.sofastream.app.data.model.MediaItem
import com.sofastream.app.data.model.MediaType
import com.sofastream.app.data.model.PlaybackInfo
import com.sofastream.app.data.model.Season
import com.sofastream.app.databinding.FragmentDetailBinding
import com.sofastream.app.ui.common.MediaRowAdapter
import com.sofastream.app.ui.player.PlayerActivity

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels()
    private val args: DetailFragmentArgs by navArgs()

    private lateinit var episodesAdapter: MediaRowAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
        viewModel.loadDetails(args.mediaItem.id)
        binding.toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerViews() {
        episodesAdapter = MediaRowAdapter { episode -> launchPlayer(episode.id) }
        binding.rvEpisodes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = episodesAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.mediaItem.observe(viewLifecycleOwner) { item ->
            bindMediaItem(item)
        }

        viewModel.seasons.observe(viewLifecycleOwner) { seasons ->
            binding.sectionSeasons.isVisible = seasons.isNotEmpty()
            setupSeasonChips(seasons)
        }

        viewModel.episodes.observe(viewLifecycleOwner) { episodes ->
            episodesAdapter.submitList(episodes)
            binding.rvEpisodes.isVisible = episodes.isNotEmpty()
        }

        viewModel.selectedSeason.observe(viewLifecycleOwner) { season ->
            season?.let { binding.tvSelectedSeason.text = it.name }
        }

        viewModel.playbackInfo.observe(viewLifecycleOwner) { info ->
            info?.let {
                launchPlayerWithInfo(it)
                viewModel.clearPlaybackInfo()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.requestSuccess.observe(viewLifecycleOwner) { success ->
            success?.let {
                val msg = if (it) getString(R.string.request_sent_success) else getString(R.string.request_sent_error)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                viewModel.clearRequestStatus()
            }
        }
    }

    private fun bindMediaItem(item: MediaItem) {
        binding.tvDetailTitle.text = item.title
        binding.tvDetailOverview.text = item.overview ?: ""
        binding.tvDetailMeta.text = buildString {
            item.year?.let { append("$it") }
            item.contentRating?.let { if (isNotEmpty()) append("  •  "); append(it) }
            val runtime = item.getRuntime()
            if (runtime.isNotEmpty()) { if (isNotEmpty()) append("  •  "); append(runtime) }
        }
        binding.tvDetailTagline.isVisible = !item.tagline.isNullOrEmpty()
        binding.tvDetailTagline.text = item.tagline

        binding.ratingBar.rating = (item.rating ?: 0f) / 2f
        binding.tvRatingValue.text = String.format("%.1f", item.rating ?: 0f)

        val imageUrl = item.backdropUrl ?: item.posterUrl
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivDetailBackdrop)
        }

        val posterUrl = item.posterUrl
        if (posterUrl != null) {
            Glide.with(this)
                .load(posterUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivDetailPoster)
        }

        item.genres?.forEach { genre ->
            val chip = Chip(context).apply {
                text = genre
                isClickable = false
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    resources.getColor(R.color.chip_background, null)
                )
                setTextColor(resources.getColor(R.color.white, null))
            }
            binding.chipGroupGenres.addView(chip)
        }

        binding.btnPlay.setOnClickListener {
            if (item.type == MediaType.SERIES) {
                val nextEpisode = viewModel.nextEpisodeToPlay()
                if (nextEpisode != null) {
                    viewModel.getPlaybackInfo(nextEpisode.id)
                }
            } else {
                viewModel.getPlaybackInfo(item.id)
            }
        }

        binding.btnRequest.setOnClickListener {
            val mediaType = if (item.type == MediaType.MOVIE) "movie" else "tv"
            viewModel.requestMedia(mediaType, 0)
            Toast.makeText(context, R.string.requesting_media, Toast.LENGTH_SHORT).show()
        }

        binding.sectionSeasons.isVisible = item.type == MediaType.SERIES
        binding.rvEpisodes.isVisible = item.type == MediaType.SERIES
    }

    private fun setupSeasonChips(seasons: List<Season>) {
        binding.chipGroupSeasons.removeAllViews()
        seasons.forEach { season ->
            val chip = Chip(context).apply {
                text = season.name
                isCheckable = true
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    resources.getColor(R.color.chip_background, null)
                )
                setTextColor(resources.getColor(R.color.white, null))
                setOnClickListener { viewModel.selectSeason(season) }
            }
            binding.chipGroupSeasons.addView(chip)
        }
        if (seasons.isNotEmpty()) {
            (binding.chipGroupSeasons.getChildAt(0) as? Chip)?.isChecked = true
        }
    }

    private fun launchPlayer(itemId: String) {
        viewModel.getPlaybackInfo(itemId)
    }

    private fun launchPlayerWithInfo(info: PlaybackInfo) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_PLAYBACK_INFO, info)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
