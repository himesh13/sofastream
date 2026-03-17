package com.sofastream.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.sofastream.app.MainActivity
import com.sofastream.app.SofaStreamApp
import com.sofastream.app.api.ApiClient
import com.sofastream.app.api.JellyfinAuthRequest
import com.sofastream.app.databinding.ActivityServerSetupBinding
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ServerSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerSetupBinding
    private val prefs by lazy { SofaStreamApp.instance.userPreferences }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (prefs.isSetupCompleteSync()) {
            navigateToMain()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener {
            val jellyfinUrl = binding.etJellyfinUrl.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val jellyseerrUrl = binding.etJellyseerrUrl.text.toString().trim()

            if (jellyfinUrl.isBlank() || username.isBlank()) {
                Toast.makeText(this, "Jellyfin URL and username are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!jellyfinUrl.startsWith("http://") && !jellyfinUrl.startsWith("https://")) {
                Toast.makeText(this, "Jellyfin URL must start with http:// or https://", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            connectToJellyfin(jellyfinUrl, username, password, jellyseerrUrl)
        }

        binding.btnSkip.setOnClickListener {
            val jellyfinUrl = binding.etJellyfinUrl.text.toString().trim()
            if (jellyfinUrl.isBlank()) {
                Toast.makeText(this, "Jellyfin URL is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!jellyfinUrl.startsWith("http://") && !jellyfinUrl.startsWith("https://")) {
                Toast.makeText(this, "Jellyfin URL must start with http:// or https://", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                prefs.saveJellyfinCredentials(jellyfinUrl, "", "default", "Guest")
                if (binding.etJellyseerrUrl.text.isNotBlank()) {
                    prefs.saveJellyseerrCredentials(binding.etJellyseerrUrl.text.toString().trim())
                }
                prefs.setSetupComplete(true)
                navigateToMain()
            }
        }
    }

    private fun connectToJellyfin(url: String, username: String, password: String, jellyseerrUrl: String) {
        binding.progressBar.isVisible = true
        binding.btnConnect.isEnabled = false

        lifecycleScope.launch {
            try {
                val api = ApiClient.getJellyfinApi(url)
                val authHeader = "MediaBrowser Client=\"SofaStream\", Device=\"Android\", DeviceId=\"sofastream-android\", Version=\"1.0\""
                val response = api.authenticateByName(authHeader, JellyfinAuthRequest(username, password))

                if (response.isSuccessful) {
                    val body = response.body() ?: run {
                        Toast.makeText(this@ServerSetupActivity, "Unexpected empty response from server", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    prefs.saveJellyfinCredentials(url, body.AccessToken, body.User.Id, body.User.Name)
                    if (jellyseerrUrl.isNotBlank()) {
                        prefs.saveJellyseerrCredentials(jellyseerrUrl)
                    }
                    prefs.setSetupComplete(true)
                    Toast.makeText(this@ServerSetupActivity, "Connected successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Toast.makeText(this@ServerSetupActivity, "Authentication failed: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: ConnectException) {
                Toast.makeText(this@ServerSetupActivity,
                    "Cannot reach server. Check the URL and ensure the server is running.",
                    Toast.LENGTH_LONG).show()
            } catch (e: SocketTimeoutException) {
                Toast.makeText(this@ServerSetupActivity,
                    "Connection timed out. Check the server address and your network.",
                    Toast.LENGTH_LONG).show()
            } catch (e: UnknownHostException) {
                Toast.makeText(this@ServerSetupActivity,
                    "Unknown host. Check the server URL.",
                    Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@ServerSetupActivity, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.isVisible = false
                binding.btnConnect.isEnabled = true
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
