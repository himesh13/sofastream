package com.sofastream.app.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.tabs.TabLayout
import com.sofastream.app.R
import com.sofastream.app.api.JellyseerrMediaDetails
import com.sofastream.app.api.JellyseerrSearchResult
import com.sofastream.app.data.model.*
import com.sofastream.app.databinding.DialogRequestMediaBinding
import com.sofastream.app.databinding.FragmentDetailBinding
import com.sofastream.app.ui.common.MediaRowAdapter
import com.sofastream.app.ui.player.PlayerActivity

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels()
    private val args: DetailFragmentArgs by navArgs()

    private lateinit var episodeAdapter: EpisodeAdapter
    private lateinit var recommendationsAdapter: MediaRowAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
        viewModel.loadDetails(args.mediaItem.id)
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { 
            requireActivity().onBackPressedDispatcher.onBackPressed() 
        }

        episodeAdapter = EpisodeAdapter { episode -> 
            viewModel.getPlaybackInfo(episode.id) 
        }
        binding.rvEpisodes.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = episodeAdapter
        }

        recommendationsAdapter = MediaRowAdapter { item ->
            // If item has a real ID (not tmdb prefix), we can navigate to its details.
            if (!item.id.startsWith("tmdb_")) {
                // Using generic navigation if Directions aren't generated yet or if Self action exists
                try {
                    // Try to navigate to self with new item
                    val action = DetailFragmentDirections.actionDetailFragmentSelf(item)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Toast.makeText(context, "Navigation error: ${item.title}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // If it's a recommendation not in library, show request info
                val tmdbId = item.tmdbId
                if (tmdbId != null) {
                    viewModel.fetchJellyseerrDetails(tmdbId, item.type == MediaType.SERIES)
                    Toast.makeText(context, R.string.loading_request_info, Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.rvRecommendations.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = recommendationsAdapter
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.layoutEpisodes.visibility = View.VISIBLE
                        binding.layoutRecommendations.visibility = View.GONE
                    }
                    1 -> {
                        binding.layoutEpisodes.visibility = View.GONE
                        binding.layoutRecommendations.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.spinnerSeasons.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val seasons = viewModel.seasons.value
                if (seasons != null && position < seasons.size) {
                    viewModel.selectSeason(seasons[position])
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, seasons.map { it.name })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSeasons.adapter = adapter
            
            val isSeries = viewModel.mediaItem.value?.type == MediaType.SERIES
            binding.tabLayout.getTabAt(0)?.view?.isVisible = isSeries
            if (!isSeries && binding.tabLayout.selectedTabPosition == 0) {
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
            }
        }

        viewModel.episodes.observe(viewLifecycleOwner) { episodes ->
            episodeAdapter.submitList(episodes)
        }

        viewModel.recommendations.observe(viewLifecycleOwner) { results ->
            val mediaItems = results.map { it.toMediaItem() }
            recommendationsAdapter.submitList(mediaItems)
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

        viewModel.jellyseerrDetails.observe(viewLifecycleOwner) { details ->
            if (details != null) {
                showRequestDialog(details)
            }
        }
    }

    private fun bindMediaItem(item: MediaItem) {
        binding.tvDetailTitle.text = item.title
        binding.tvDetailMeta.text = buildString {
            item.year?.let { append("$it") }
            item.contentRating?.let { if (isNotEmpty()) append("  •  "); append(it) }
            val runtime = item.getRuntime()
            if (runtime.isNotEmpty()) { if (isNotEmpty()) append("  •  "); append(runtime) }
        }
        binding.tvDetailOverview.text = item.overview ?: ""

        val castText = item.cast?.take(3)?.joinToString { it.name }
        if (!castText.isNullOrEmpty()) {
            binding.tvCastLabel.text = "Cast: $castText..."
            binding.tvCastLabel.visibility = View.VISIBLE
        } else {
            binding.tvCastLabel.visibility = View.GONE
        }

        val imageUrl = item.backdropUrl ?: item.posterUrl
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivDetailBackdrop)
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
            val tmdbId = item.tmdbId
            if (tmdbId != null) {
                viewModel.fetchJellyseerrDetails(tmdbId, item.type == MediaType.SERIES)
                Toast.makeText(context, R.string.loading_request_info, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "TMDB ID not found for this item", Toast.LENGTH_SHORT).show()
            }
        }

        val isSeries = item.type == MediaType.SERIES
        binding.layoutEpisodes.isVisible = isSeries
        binding.tabLayout.isVisible = true 
    }

    private fun showRequestDialog(details: JellyseerrMediaDetails) {
        val dialogBinding = DialogRequestMediaBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_SofaStream_Dialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvRequestTitle.text = details.name ?: details.title
        
        val isTv = details.seasons != null
        dialogBinding.layoutTableHeaders.isVisible = isTv
        dialogBinding.rvRequestSeasons.isVisible = isTv

        var selectedSeasons = emptyList<Int>()

        if (isTv && details.seasons != null) {
            val seasonsWithStatus = details.seasons.map { season ->
                val status = details.mediaInfo?.seasons?.find { it.seasonNumber == season.seasonNumber }?.status
                season.copy(status = status ?: season.status)
            }

            val adapter = RequestSeasonsAdapter(seasonsWithStatus) { selected ->
                selectedSeasons = selected
            }
            dialogBinding.rvRequestSeasons.layoutManager = LinearLayoutManager(context)
            dialogBinding.rvRequestSeasons.adapter = adapter
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnSubmitRequest.setOnClickListener {
            val mediaType = if (isTv) "tv" else "movie"
            val tmdbId = details.id
            val tvdbId = details.mediaInfo?.tvdbId
            
            if (isTv && selectedSeasons.isEmpty()) {
                Toast.makeText(context, "Please select at least one season", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.requestMedia(mediaType, tmdbId, tvdbId, if (isTv) selectedSeasons else null)
            dialog.dismiss()
            Toast.makeText(context, R.string.requesting_media, Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun launchPlayerWithInfo(info: PlaybackInfo) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_PLAYBACK_INFO, info)
        }
        startActivity(intent)
    }

    private fun JellyseerrSearchResult.toMediaItem(): MediaItem {
        return MediaItem(
            id = "tmdb_${this.id}", 
            title = this.title ?: this.name ?: "",
            overview = this.overview,
            type = if (this.mediaType == "tv") MediaType.SERIES else MediaType.MOVIE,
            year = (this.releaseDate ?: this.firstAirDate)?.take(4)?.toIntOrNull(),
            rating = this.voteAverage?.toFloat(),
            contentRating = null,
            runtimeTicks = null,
            backdropUrl = if (this.backdropPath != null) "https://image.tmdb.org/t/p/w780${this.backdropPath}" else null,
            posterUrl = if (this.posterPath != null) "https://image.tmdb.org/t/p/w500${this.posterPath}" else null,
            thumbUrl = null,
            genres = null,
            studios = null,
            seriesId = null,
            seriesName = null,
            seasonId = null,
            episodeNumber = null,
            seasonNumber = null,
            tagline = null,
            cast = null,
            providerIds = mapOf("Tmdb" to this.id.toString())
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
