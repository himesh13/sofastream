package com.sofastream.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sofastream.app.SofaStreamApp
import com.sofastream.app.api.ApiClient
import com.sofastream.app.databinding.FragmentSettingsBinding
import com.sofastream.app.ui.onboarding.ServerSetupActivity
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy { SofaStreamApp.instance.userPreferences }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadCurrentSettings()
        setupButtons()
    }

    private fun loadCurrentSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch { prefs.jellyfinUrl.collect { url -> binding.etJellyfinUrl.setText(url) } }
                launch { prefs.jellyseerrUrl.collect { url -> binding.etJellyseerrUrl.setText(url) } }
                launch { prefs.jellyfinUserName.collect { name ->
                    binding.tvConnectedUser.text = if (name.isNotEmpty()) "Connected as: $name" else "Not connected"
                }}
            }
        }
    }

    private fun setupButtons() {
        binding.btnSaveSettings.setOnClickListener {
            val jellyfinUrl = binding.etJellyfinUrl.text.toString().trim()
            val jellyseerrUrl = binding.etJellyseerrUrl.text.toString().trim()

            if (jellyfinUrl.isBlank()) {
                Toast.makeText(context, "Jellyfin URL is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                if (jellyseerrUrl.isNotBlank()) {
                    prefs.saveJellyseerrCredentials(jellyseerrUrl)
                }
                ApiClient.resetClients()
                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                prefs.clearAll()
                ApiClient.resetClients()
                val intent = Intent(requireContext(), ServerSetupActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
