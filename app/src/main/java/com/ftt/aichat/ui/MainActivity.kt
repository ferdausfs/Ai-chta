package com.ftt.aichat.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ftt.aichat.R
import com.ftt.aichat.adapter.ChatAdapter
import com.ftt.aichat.databinding.ActivityMainBinding
import com.ftt.aichat.viewmodel.ChatViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupInputArea()
        observeViewModel()

        // Prompt to set API key on first launch
        if (!viewModel.hasApiKey) {
            Toast.makeText(this, "Please set your API key in Settings", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh model name in subtitle after returning from settings
        viewModel.refreshModel()
    }

    // ── Setup ─────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(this)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // messages grow from bottom
        }
        binding.recyclerView.apply {
            this.layoutManager = layoutManager
            adapter = chatAdapter
            itemAnimator = null // disable default animation for smoother streaming
        }
    }

    private fun setupInputArea() {
        // Send button click
        binding.btnSend.setOnClickListener {
            val input = binding.etInput.text?.toString()?.trim() ?: ""
            if (input.isNotEmpty()) {
                viewModel.sendMessage(input)
                binding.etInput.text?.clear()
            }
        }

        // Stop button click (cancel streaming)
        binding.btnStop.setOnClickListener {
            viewModel.stopStreaming()
        }
    }

    // ── Observe ──────────────────────────────────────────────────

    private fun observeViewModel() {
        // Messages list
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages.toList()) {
                // Auto-scroll to bottom after list update
                if (messages.isNotEmpty()) {
                    binding.recyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }

            // Show/hide empty state
            binding.layoutEmpty.visibility =
                if (messages.isEmpty()) View.VISIBLE else View.GONE
        }

        // Streaming state — toggle send/stop buttons
        viewModel.isStreaming.observe(this) { isStreaming ->
            binding.btnSend.visibility = if (isStreaming) View.GONE else View.VISIBLE
            binding.btnStop.visibility = if (isStreaming) View.VISIBLE else View.GONE
            binding.etInput.isEnabled = !isStreaming
        }

        // Error messages
        viewModel.error.observe(this) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Model name in toolbar subtitle
        viewModel.selectedModel.observe(this) { model ->
            supportActionBar?.subtitle = model.displayName
        }
    }

    // ── Toolbar Menu ──────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_chat -> {
                viewModel.clearChat()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
