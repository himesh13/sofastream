package com.sofastream.app.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.sofastream.app.data.model.MediaItem
import com.sofastream.app.databinding.FragmentSearchBinding
import com.sofastream.app.ui.common.MediaGridAdapter

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: MediaGridAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupSearch()
    }

    private fun setupRecyclerView() {
        adapter = MediaGridAdapter { item -> navigateToDetail(item) }
        binding.rvSearchResults.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = this@SearchFragment.adapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.search(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }

        viewModel.results.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            binding.tvNoResults.isVisible = results.isEmpty() && !binding.etSearch.text.isNullOrEmpty()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            binding.tvError.isVisible = error != null
            binding.tvError.text = error
        }
    }

    private fun navigateToDetail(item: MediaItem) {
        val action = SearchFragmentDirections.actionSearchToDetail(item)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
