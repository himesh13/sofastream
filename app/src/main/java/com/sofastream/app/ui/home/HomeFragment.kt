package com.sofastream.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sofastream.app.R
import com.sofastream.app.data.model.MediaItem
import com.sofastream.app.databinding.FragmentHomeBinding
import com.sofastream.app.ui.common.MediaRowAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var continueWatchingAdapter: MediaRowAdapter
    private lateinit var recentMoviesAdapter: MediaRowAdapter
    private lateinit var recentSeriesAdapter: MediaRowAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
        viewModel.loadHomeContent()

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadHomeContent()
        }
    }

    private fun setupRecyclerViews() {
        continueWatchingAdapter = MediaRowAdapter { item -> navigateToDetail(item) }
        recentMoviesAdapter = MediaRowAdapter { item -> navigateToDetail(item) }
        recentSeriesAdapter = MediaRowAdapter { item -> navigateToDetail(item) }

        binding.rvContinueWatching.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = continueWatchingAdapter
        }

        binding.rvRecentMovies.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentMoviesAdapter
        }

        binding.rvRecentSeries.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentSeriesAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
            binding.progressBar.isVisible = isLoading && continueWatchingAdapter.itemCount == 0
        }

        viewModel.featuredItem.observe(viewLifecycleOwner) { item ->
            item?.let { updateFeaturedBanner(it) }
        }

        viewModel.continueWatching.observe(viewLifecycleOwner) { items ->
            binding.sectionContinueWatching.isVisible = items.isNotEmpty()
            continueWatchingAdapter.submitList(items)
        }

        viewModel.recentMovies.observe(viewLifecycleOwner) { items ->
            binding.sectionRecentMovies.isVisible = items.isNotEmpty()
            recentMoviesAdapter.submitList(items)
        }

        viewModel.recentSeries.observe(viewLifecycleOwner) { items ->
            binding.sectionRecentSeries.isVisible = items.isNotEmpty()
            recentSeriesAdapter.submitList(items)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.tvError.isVisible = error != null
            binding.tvError.text = error
        }
    }

    private fun updateFeaturedBanner(item: MediaItem) {
        binding.tvFeaturedTitle.text = item.title
        binding.tvFeaturedMeta.text = buildString {
            item.year?.let { append("$it") }
            item.contentRating?.let { if (isNotEmpty()) append("  •  "); append(it) }
            if (item.getRuntime().isNotEmpty()) { if (isNotEmpty()) append("  •  "); append(item.getRuntime()) }
        }

        val imageUrl = item.backdropUrl ?: item.posterUrl
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivFeaturedBanner)
        }

        binding.btnFeaturedPlay.setOnClickListener { navigateToDetail(item) }
        binding.btnFeaturedInfo.setOnClickListener { navigateToDetail(item) }
        binding.featuredBannerContainer.setOnClickListener { navigateToDetail(item) }
    }

    private fun navigateToDetail(item: MediaItem) {
        val action = HomeFragmentDirections.actionHomeToDetail(item)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
